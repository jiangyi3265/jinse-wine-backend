#!/usr/bin/env python3
"""Export the authorized production MySQL/MinIO state; transfer only ciphertext.

Requires the existing Linux systemd services and cryptography. The private RSA
key is never sent to this host. Does not restore or change business records.
"""
import argparse
import base64
import datetime
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import struct
import subprocess
import sys
import time
from urllib.parse import urlsplit
from urllib.request import urlopen
import zipfile
import xml.etree.ElementTree as ET
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from snapshot_storage import Storage, object_metadata, sha


def export(output, public_key_path):
    os.umask(0o077)
    output = Path(output).resolve()
    if output.parent != Path('/www/backup/jinse-wine') or not re.fullmatch(r'production-joint-\d{8}-\d{6}-[a-f0-9]{8}', output.name):
        raise ValueError('Unexpected backup directory')
    output.mkdir(mode=0o700, exist_ok=False)
    public_key = serialization.load_pem_public_key(Path(public_key_path).read_bytes())
    if not isinstance(public_key, rsa.RSAPublicKey) or public_key.key_size < 3072:
        raise ValueError('A 3072-bit or stronger RSA public key is required')
    pid = subprocess.check_output(['systemctl', 'show', '-p', 'MainPID', '--value', 'jinse-wine.service'], text=True).strip()
    if not pid.isdigit() or pid == '0':
        raise RuntimeError('Production Java service is not running; no service was stopped')
    environment = dict(x.split('=', 1) for x in Path('/proc/' + pid + '/environ').read_bytes().decode().split('\0') if '=' in x)
    args = Path('/proc/' + pid + '/cmdline').read_bytes().decode().split('\0')
    jar = Path(next(x for x in args if x.endswith('.jar'))).resolve()
    settings = dict(line.split('=', 1) for line in Path('/etc/jinse-wine/application.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
    db_url = environment['DB_URL']
    database = urlsplit(db_url[5:] if db_url.startswith('jdbc:') else db_url)
    if database.hostname not in ('localhost', '127.0.0.1') or database.path != '/jinse_wine':
        raise RuntimeError('Unexpected production database')
    if settings.get('storage.minio.enabled') != 'true' or settings.get('storage.minio.endpoint') != 'http://127.0.0.1:9000':
        raise RuntimeError('Unexpected production object storage')
    effective_storage = {'MINIO_ENABLED':'storage.minio.enabled','MINIO_ENDPOINT':'storage.minio.endpoint',
                         'MINIO_ACCESS_KEY':'storage.minio.access-key','MINIO_SECRET_KEY':'storage.minio.secret-key','MINIO_BUCKET':'storage.minio.bucket'}
    if any(environment.get(variable, settings.get(property_name)) != settings.get(property_name) for variable,property_name in effective_storage.items()):
        raise RuntimeError('Running Java storage configuration differs from the snapshot configuration; inspect effective settings first')
    bucket = settings['storage.minio.bucket']
    storage = Storage(settings['storage.minio.endpoint'], settings['storage.minio.access-key'], settings['storage.minio.secret-key'])
    db_args = ['--host=' + database.hostname, '--port=' + str(database.port or 3306),
               '--user=' + environment['DB_USERNAME'], '--default-character-set=utf8mb4']
    db_env = dict(os.environ, MYSQL_PWD=environment['DB_PASSWORD'])
    mysql = shutil.which('mysql') or '/www/server/mysql/bin/mysql'
    dump = shutil.which('mysqldump') or '/www/server/mysql/bin/mysqldump'

    def query(statement):
        result = subprocess.run([mysql, *db_args, '--database=jinse_wine', '--batch', '--skip-column-names', '--raw', '--execute=' + statement],
                                env=db_env, capture_output=True, timeout=30)
        if result.returncode:
            raise RuntimeError('Production snapshot database read failed; private diagnostics withheld')
        return result.stdout.decode('utf-8').strip()

    # Restore is intentionally inert: no SQL event, routine, trigger, or view code.
    for kind, condition in [('tables', "table_type<>'BASE TABLE' OR engine<>'InnoDB'"), ('events', '1=1'), ('routines', '1=1'), ('triggers', '1=1')]:
        schema_column = {'tables': 'table_schema', 'triggers': 'trigger_schema', 'events': 'event_schema', 'routines': 'routine_schema'}[kind]
        if query("SELECT COUNT(*) FROM information_schema." + kind + " WHERE " + schema_column + "=DATABASE() AND (" + condition + ')') != '0':
            raise RuntimeError('Unsupported executable SQL objects or non-InnoDB tables; snapshot was not started')
    versioning = storage.request('GET', bucket, query={'versioning': ''})[0]
    if any(node.tag.rsplit('}', 1)[-1] == 'Status' and node.text in ('Enabled', 'Suspended') for node in ET.fromstring(versioning).iter()):
        raise RuntimeError('Versioned buckets require a version-aware backup tool')
    initial_objects = storage.objects(bucket)
    if sum(item['bytes'] for item in initial_objects) > 64 * 1024 * 1024:
        raise RuntimeError('Object data exceeds this bounded maintenance export; use streaming backup')
    tables = query("SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() ORDER BY table_name").splitlines()
    if not tables or any(not re.fullmatch(r'[A-Za-z0-9_]+', name) for name in tables):
        raise RuntimeError('Unexpected database table inventory')
    sql_file, archive_path = output / 'database.sql', output / 'snapshot.zip'
    stopped = False
    start = time.monotonic()
    manifest = None
    watchdog = None
    try:
        watchdog = subprocess.Popen([sys.executable, '-c', "import subprocess,time; time.sleep(150); subprocess.run(['systemctl','start','jinse-wine.service'])"],
                                    stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, start_new_session=True)
        stopped = True
        subprocess.run(['systemctl', 'stop', 'jinse-wine.service'], check=True, timeout=35)
        if subprocess.check_output(['systemctl', 'show', '-p', 'MainPID', '--value', 'jinse-wine.service'], text=True).strip() != '0':
            raise RuntimeError('Java writer did not stop')

        def table_state():
            counts = {name: int(query('SELECT COUNT(*) FROM `' + name + '`')) for name in tables}
            checksums = query('CHECKSUM TABLE ' + ','.join('`' + name + '`' for name in tables) + ' EXTENDED')
            if any(row.endswith('\tNULL') for row in checksums.splitlines()):
                raise RuntimeError('Database checksums are unavailable')
            return counts, checksums

        before_counts, before_checksums = table_state()
        before_objects = storage.objects(bucket)
        if before_objects != initial_objects:
            raise RuntimeError('Storage changed before maintenance; retry with a fresh preflight')
        columns = query("SELECT table_name,column_name,column_type,is_nullable FROM information_schema.columns WHERE table_schema=DATABASE() ORDER BY table_name,ordinal_position")
        indexes = query("SELECT table_name,index_name,seq_in_index,column_name,non_unique FROM information_schema.statistics WHERE table_schema=DATABASE() ORDER BY table_name,index_name,seq_in_index")
        with sql_file.open('wb') as destination:
            result = subprocess.run([dump, *db_args, '--single-transaction', '--hex-blob', '--set-gtid-purged=OFF', '--no-tablespaces', 'jinse_wine'],
                                    env=db_env, stdout=destination, stderr=subprocess.PIPE, timeout=45)
        if result.returncode:
            raise RuntimeError('Production MySQL dump failed; private diagnostics withheld')
        manifest = {'format': 1, 'createdAt': datetime.datetime.now().astimezone().isoformat(),
                    'source': 'production MySQL and MinIO', 'database': 'jinse_wine', 'bucket': bucket,
                    'tables': before_counts, 'columns': columns, 'indexes': indexes, 'sqlSha256': sha(sql_file.read_bytes()),
                    'backendJarSha256': sha(jar.read_bytes()), 'objects': []}
        with zipfile.ZipFile(archive_path, 'x', zipfile.ZIP_DEFLATED) as archive:
            archive.write(sql_file, 'database.sql')
            for index, item in enumerate(before_objects):
                if time.monotonic() - start > 90:
                    raise RuntimeError('Maintenance time limit reached; service will resume')
                data, headers = storage.request('GET', bucket, item['key'])
                if len(data) != item['bytes'] or headers.get('etag') != item['etag']:
                    raise RuntimeError('Object changed while snapshotting')
                filename = 'objects/' + str(index) + '.bin'
                archive.writestr(filename, data)
                tags = storage.request('GET', bucket, item['key'], query={'tagging': ''})[0].decode('utf-8')
                manifest['objects'].append({**item, 'file': filename, 'sha256': sha(data), 'metadata': object_metadata(headers), 'tags': tags})
            after_counts, after_checksums = table_state()
            if before_counts != after_counts or before_checksums != after_checksums or before_objects != storage.objects(bucket):
                raise RuntimeError('Source changed during maintenance; joint snapshot rejected')
            manifest['sourceStable'] = True
            archive.writestr('manifest.json', json.dumps(manifest, ensure_ascii=False, indent=2))
    finally:
        if stopped:
            subprocess.run(['systemctl', 'start', 'jinse-wine.service'], check=True, timeout=35)
            port = next((x.split('=', 1)[1] for x in args if x.startswith('--server.port=')), environment.get('SERVER_PORT', '8080'))
            ready = False
            for attempt in range(40):
                try:
                    with urlopen('http://127.0.0.1:' + port + '/mini/products', timeout=2) as response:
                        ready = json.load(response).get('code') == 0
                        if ready:
                            break
                except Exception:
                    time.sleep(0.5)
            if not ready:
                raise RuntimeError('Java was restarted but did not become healthy; inspect the service immediately')
            if watchdog:
                watchdog.terminate()
                watchdog.wait(timeout=5)
    downtime = round(time.monotonic() - start, 2)
    plaintext = archive_path.read_bytes()
    if len(plaintext) > 80 * 1024 * 1024:
        raise RuntimeError('Archive exceeds the bounded transfer size')
    key, nonce = secrets.token_bytes(32), secrets.token_bytes(12)
    wrapped = public_key.encrypt(key, padding.OAEP(mgf=padding.MGF1(hashes.SHA256()), algorithm=hashes.SHA256(), label=None))
    header = json.dumps({'format': 1, 'cipher': 'AES-256-GCM', 'keyWrap': 'RSA-OAEP-SHA256',
                         'wrappedKey': base64.b64encode(wrapped).decode(), 'nonce': base64.b64encode(nonce).decode(),
                         'plaintextSha256': sha(plaintext), 'plaintextBytes': len(plaintext)}, separators=(',', ':')).encode()
    aad = b'JINSE1\n' + struct.pack('>I', len(header)) + header
    ciphertext = aad + AESGCM(key).encrypt(nonce, plaintext, aad)
    encrypted = output / 'joint-snapshot.enc'
    encrypted.write_bytes(ciphertext)
    chunk_size = 384 * 1024
    chunks = []
    for index, offset in enumerate(range(0, len(ciphertext), chunk_size)):
        filename = 'chunk-' + str(index).zfill(4) + '.b64'
        chunk = ciphertext[offset:offset + chunk_size]
        (output / filename).write_bytes(base64.b64encode(chunk))
        chunks.append({'file': filename, 'bytes': len(chunk), 'sha256': sha(chunk)})
    receipt = {'status': 'success', 'backup': str(output), 'encryptedBytes': len(ciphertext),
               'encryptedSha256': sha(ciphertext), 'plaintextSha256': sha(plaintext), 'chunks': chunks,
               'tables': len(manifest['tables']), 'rows': sum(manifest['tables'].values()),
               'objects': len(manifest['objects']), 'objectBytes': sum(item['bytes'] for item in manifest['objects']),
               'sourceStable': True, 'javaHealthy': True, 'maintenanceSeconds': downtime}
    (output / 'receipt.json').write_text(json.dumps(receipt, indent=2))
    archive_path.unlink()
    sql_file.unlink()
    print(json.dumps({key: value for key, value in receipt.items() if key != 'chunks'}), flush=True)


if __name__ == '__main__':
    import fcntl
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', required=True)
    parser.add_argument('--public-key', required=True)
    args = parser.parse_args()
    with Path('/run/lock/jinse-wine-joint-backup.lock').open('w') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        try:
            export(args.output, args.public_key)
        finally:
            directory = Path(args.output).resolve()
            if directory.parent == Path('/www/backup/jinse-wine') and re.fullmatch(r'production-joint-\d{8}-\d{6}-[a-f0-9]{8}', directory.name):
                for name in ('database.sql', 'snapshot.zip'):
                    path = directory / name
                    if path.is_file() and not path.is_symlink():
                        path.unlink()
