#!/usr/bin/env python3
"""Snapshot local MySQL and MinIO while Java is stopped, restore into isolated names.

Uses Python's standard library and mysql/mysqldump on PATH. Load the existing
local-development.ps1 and minio-local.ps1 credentials into the environment first.
Never targets production. Leaves a private snapshot in ignored backups/.
"""
import datetime
import hashlib
import hmac
import json
import os
from pathlib import Path
import re
import shutil
import socket
import subprocess
import uuid
from urllib.parse import quote, urlencode, urlsplit
from urllib.request import Request, urlopen
import xml.etree.ElementTree as ET
import zipfile

BACKEND = Path(__file__).resolve().parents[1]
SOURCE_DB = 'jinse_wine'
SOURCE_BUCKET = os.environ.get('MINIO_BUCKET', 'jinse-wine')
ENDPOINT = os.environ.get('MINIO_ENDPOINT', 'http://127.0.0.1:9000').rstrip('/')
parsed = urlsplit(ENDPOINT)
if parsed.scheme != 'http' or parsed.hostname not in ('localhost', '127.0.0.1') or parsed.port != 9000 or parsed.path:
    raise SystemExit('This recovery exercise only permits local MinIO on port 9000')
dburl = urlsplit(os.environ.get('DB_URL', '').removeprefix('jdbc:'))
if dburl.hostname not in ('localhost', '127.0.0.1') or dburl.port != 3306 or dburl.path != '/jinse_wine':
    raise SystemExit('Load the local jinse_wine database configuration first')
with socket.socket() as sock:
    sock.settimeout(1)
    if sock.connect_ex(('127.0.0.1', 8080)) == 0:
        raise SystemExit('Stop only the local project Java service before taking a joint snapshot')
access = os.environ.get('MINIO_ROOT_USER') or os.environ.get('MINIO_ACCESS_KEY')
secret = os.environ.get('MINIO_ROOT_PASSWORD') or os.environ.get('MINIO_SECRET_KEY')
if not access or not secret or not os.environ.get('DB_PASSWORD'):
    raise SystemExit('Local credentials are missing; values withheld')
mysql = shutil.which('mysql.exe') or shutil.which('mysql')
dump = shutil.which('mysqldump.exe') or shutil.which('mysqldump')
if not mysql or not dump:
    raise SystemExit('mysql and mysqldump must be on PATH')
dbargs = ['--protocol=tcp', '--host=127.0.0.1', '--port=3306',
          '--user=' + os.environ['DB_USERNAME'], '--default-character-set=utf8mb4']
dbenv = dict(os.environ, MYSQL_PWD=os.environ['DB_PASSWORD'])
digest = lambda b: hashlib.sha256(b).hexdigest()
mac = lambda k, s: hmac.new(k, s.encode(), hashlib.sha256).digest()


def s3(method, bucket, key='', data=b'', query=None, metadata=None):
    stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    date = stamp[:8]
    path = '/' + quote(bucket, safe='') + ('/' + quote(key, safe='/') if key else '')
    query_text = urlencode(sorted((query or {}).items()), quote_via=quote, safe='')
    headers = {'host': parsed.netloc, 'x-amz-date': stamp, 'x-amz-content-sha256': digest(data)}
    headers.update(metadata or {})
    names = ';'.join(sorted(headers))
    canonical = '\n'.join([method, path, query_text,
                           ''.join(k + ':' + str(headers[k]).strip() + '\n' for k in sorted(headers)), names, digest(data)])
    scope = date + '/us-east-1/s3/aws4_request'
    signing = mac(mac(mac(mac(('AWS4' + secret).encode(), date), 'us-east-1'), 's3'), 'aws4_request')
    signature = mac(signing, 'AWS4-HMAC-SHA256\n' + stamp + '\n' + scope + '\n' + digest(canonical.encode())).hex()
    headers['Authorization'] = 'AWS4-HMAC-SHA256 Credential=' + access + '/' + scope + ', SignedHeaders=' + names + ', Signature=' + signature
    url = ENDPOINT + path + ('?' + query_text if query_text else '')
    with urlopen(Request(url, data=data if method in ('PUT', 'POST') else None, headers=headers, method=method), timeout=20) as response:
        return response.read(), {k.lower(): v for k, v in response.headers.items()}


def objects(bucket):
    result, token = [], None
    ns = {'s': 'http://s3.amazonaws.com/doc/2006-03-01/'}
    while True:
        query = {'list-type': '2'}
        if token: query['continuation-token'] = token
        root = ET.fromstring(s3('GET', bucket, query=query)[0])
        for item in root.findall('s:Contents', ns):
            result.append({'key': item.findtext('s:Key', namespaces=ns),
                           'bytes': int(item.findtext('s:Size', namespaces=ns)),
                           'etag': item.findtext('s:ETag', namespaces=ns)})
        if root.findtext('s:IsTruncated', namespaces=ns) != 'true': break
        token = root.findtext('s:NextContinuationToken', namespaces=ns)
        if not token: raise RuntimeError('Missing object listing continuation token')
    return sorted(result, key=lambda item: item['key'])


def query(database, sql):
    result = subprocess.run([mysql, *dbargs, '--batch', '--skip-column-names', '--raw',
                             *(['--database=' + database] if database else []), '--execute=' + sql],
                            env=dbenv, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode: raise RuntimeError('Local database operation failed; diagnostic may contain private data and was withheld')
    return result.stdout.decode('utf-8').strip()


def row_counts(database):
    names = query(database, 'SHOW TABLES').splitlines()
    result = {}
    for name in names:
        if not re.fullmatch('[A-Za-z0-9_]+', name): raise RuntimeError('Unexpected table name')
        result[name] = int(query(database, 'SELECT COUNT(*) FROM `' + name + '`'))
    return result


suffix = uuid.uuid4().hex
temporary_db = 'jinse_recovery_' + suffix
temporary_bucket = 'jinse-recovery-' + suffix
assert re.fullmatch('jinse_recovery_[a-f0-9]{32}', temporary_db)
assert re.fullmatch('jinse-recovery-[a-f0-9]{32}', temporary_bucket)
assert temporary_db != SOURCE_DB and temporary_bucket != SOURCE_BUCKET
output = BACKEND / 'backups' / ('joint-' + datetime.datetime.now().strftime('%Y%m%d-%H%M%S') + '-' + suffix[:8])
output.mkdir(parents=True, exist_ok=False)
sql_file = output / 'database.sql'
archive_path = output / 'joint-snapshot.zip'
db_created = bucket_created = False
uploaded = []
report = None
try:
    if query('', "SELECT COUNT(*) FROM information_schema.events WHERE event_schema='jinse_wine'") != '0':
        raise RuntimeError('This local exercise requires a database without scheduled SQL events')
    before_counts = row_counts(SOURCE_DB)
    before_objects = objects(SOURCE_BUCKET)
    result = subprocess.run([dump, *dbargs, '--single-transaction', '--routines', '--triggers', '--events',
                             '--hex-blob', '--set-gtid-purged=OFF', '--no-tablespaces',
                             '--result-file=' + str(sql_file), SOURCE_DB], env=dbenv, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode: raise RuntimeError('Local database export failed')
    manifest = {'createdAt': datetime.datetime.now().astimezone().isoformat(), 'database': SOURCE_DB,
                'bucket': SOURCE_BUCKET, 'tables': before_counts, 'sqlSha256': digest(sql_file.read_bytes()), 'objects': []}
    with zipfile.ZipFile(archive_path, 'x', zipfile.ZIP_DEFLATED) as archive:
        archive.write(sql_file, 'database.sql')
        for index, entry in enumerate(before_objects):
            data, headers = s3('GET', SOURCE_BUCKET, entry['key'])
            if len(data) != entry['bytes']: raise RuntimeError('Source object changed during snapshot')
            metadata = {k: v for k, v in headers.items() if k.startswith('x-amz-meta-') or k in
                        ('content-type', 'content-disposition', 'content-encoding', 'cache-control')}
            filename = 'objects/' + str(index) + '.bin'
            archive.writestr(filename, data)
            manifest['objects'].append({**entry, 'file': filename, 'sha256': digest(data), 'metadata': metadata})
        if row_counts(SOURCE_DB) != before_counts or objects(SOURCE_BUCKET) != before_objects:
            raise RuntimeError('Local source changed during snapshot; retry during a quiet maintenance window')
        archive.writestr('manifest.json', json.dumps(manifest, ensure_ascii=False, indent=2))
    query('', 'CREATE DATABASE ' + temporary_db + ' CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci')
    db_created = True
    query(temporary_db, 'source ' + sql_file.as_posix())
    if row_counts(temporary_db) != before_counts: raise RuntimeError('Restored table counts differ')
    province = query('', "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='" + temporary_db
                     + "' AND table_name='biz_store' AND column_name IN ('province','city','district')")
    if province != '3': raise RuntimeError('Restored region schema is incomplete')
    s3('PUT', temporary_bucket)
    bucket_created = True
    total_bytes = 0
    with zipfile.ZipFile(archive_path) as archive:
        if digest(archive.read('database.sql')) != manifest['sqlSha256']: raise RuntimeError('Archived SQL checksum differs')
        for entry in manifest['objects']:
            data = archive.read(entry['file'])
            if digest(data) != entry['sha256']: raise RuntimeError('Archived object checksum differs')
            s3('PUT', temporary_bucket, entry['key'], data=data, metadata=entry['metadata'])
            uploaded.append(entry['key'])
            restored, headers = s3('GET', temporary_bucket, entry['key'])
            if digest(restored) != entry['sha256']: raise RuntimeError('Restored object checksum differs')
            if any(headers.get(k) != v for k, v in entry['metadata'].items()): raise RuntimeError('Restored object metadata differs')
            total_bytes += len(restored)
    if len(objects(temporary_bucket)) != len(before_objects): raise RuntimeError('Restored object count differs')
    report = {'completedAt': datetime.datetime.now().astimezone().isoformat(), 'scope':'local isolated recovery',
              'archive': str(archive_path), 'sha256': digest(archive_path.read_bytes()),
              'tables': len(before_counts), 'rows': sum(before_counts.values()), 'objects':len(before_objects),
              'objectBytes': total_bytes, 'allObjectHashesAndMetadataMatched':True, 'storeRegionColumns':3}
finally:
    # Only remove isolated resources created by this exact run, never source data.
    cleanup_errors = []
    if bucket_created:
        try:
            for key in uploaded: s3('DELETE', temporary_bucket, key)
            s3('DELETE', temporary_bucket)
        except Exception: cleanup_errors.append('temporary MinIO bucket cleanup failed: ' + temporary_bucket)
    if db_created:
        try: query('', 'DROP DATABASE ' + temporary_db)
        except Exception: cleanup_errors.append('temporary database cleanup failed: ' + temporary_db)
    if cleanup_errors: raise RuntimeError('; '.join(cleanup_errors))
if report:
    report['temporaryResourcesRemoved'] = True
    (output/'recovery-receipt.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps(report))
