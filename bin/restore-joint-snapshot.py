#!/usr/bin/env python3
"""Restore a decrypted, authenticated snapshot into random local names only."""
import argparse
import datetime
import csv
import io
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
from urllib.parse import urlsplit
import uuid
import xml.etree.ElementTree as ET
import zipfile
from snapshot_storage import Storage, object_metadata, sha


def schema_shape(columns, indexes):
    # information_schema table/index sorting differs between MySQL versions.
    # Preserve column order within each table and index sequence within each row.
    grouped = {}
    for row in csv.reader(io.StringIO(columns), delimiter='\t', quoting=csv.QUOTE_NONE):
        grouped.setdefault(row[0], []).append(row[1:])
    return grouped, sorted(csv.reader(io.StringIO(indexes), delimiter='\t', quoting=csv.QUOTE_NONE))


def restore(archive_path, receipt_path, application_check=None):
    backend = Path(__file__).resolve().parents[1]
    archive_path, receipt_path = Path(archive_path).resolve(), Path(receipt_path).resolve()
    backup_root = (backend / 'backups').resolve()
    if backup_root not in archive_path.parents or backup_root not in receipt_path.parents:
        raise ValueError('Snapshot and receipt must stay within the private backups directory')
    if receipt_path.exists():
        raise ValueError('Recovery receipt already exists')
    db_url = os.environ.get('DB_URL', '')
    database = urlsplit(db_url[5:] if db_url.startswith('jdbc:') else db_url)
    endpoint = urlsplit(os.environ.get('MINIO_ENDPOINT', ''))
    if database.hostname not in ('localhost', '127.0.0.1') or (database.port or 3306) != 3306 or database.path != '/jinse_wine':
        raise ValueError('Recovery requires the local development MySQL configuration')
    if endpoint.scheme != 'http' or endpoint.hostname not in ('localhost', '127.0.0.1') or endpoint.port != 9000 or endpoint.path:
        raise ValueError('Recovery requires local MinIO on port 9000')
    if not os.environ.get('DB_PASSWORD'):
        raise ValueError('Local database password is missing; value withheld')
    storage = Storage(os.environ['MINIO_ENDPOINT'], os.environ.get('MINIO_ROOT_USER') or os.environ.get('MINIO_ACCESS_KEY'),
                      os.environ.get('MINIO_ROOT_PASSWORD') or os.environ.get('MINIO_SECRET_KEY'))
    mysql = shutil.which('mysql.exe') or shutil.which('mysql')
    if not mysql:
        raise RuntimeError('mysql must be on PATH')
    db_args = ['--protocol=tcp', '--host=127.0.0.1', '--port=3306', '--user=' + os.environ['DB_USERNAME'], '--default-character-set=utf8mb4']
    db_env = dict(os.environ, MYSQL_PWD=os.environ['DB_PASSWORD'])

    def query(statement, db=None):
        result = subprocess.run([mysql, *db_args, '--batch', '--skip-column-names', '--raw', *(['--database=' + db] if db else []), '--execute=' + statement],
                                env=db_env, capture_output=True, timeout=60)
        if result.returncode:
            raise RuntimeError('Isolated recovery database operation failed; private diagnostics withheld')
        return result.stdout.decode('utf-8').strip()

    def tag_values(xml):
        ns = {'s': 'http://s3.amazonaws.com/doc/2006-03-01/'}
        return sorted((node.findtext('s:Key', namespaces=ns), node.findtext('s:Value', namespaces=ns)) for node in ET.fromstring(xml).findall('.//s:Tag', ns))

    suffix = uuid.uuid4().hex
    temporary_db = 'jinse_offsite_' + suffix
    temporary_bucket = 'jinse-offsite-' + suffix
    assert re.fullmatch(r'jinse_offsite_[a-f0-9]{32}', temporary_db)
    assert re.fullmatch(r'jinse-offsite-[a-f0-9]{32}', temporary_bucket)
    db_created = bucket_created = False
    uploaded = []
    sql_path = archive_path.parent / ('restore-' + suffix + '.sql')
    result = None
    try:
        with zipfile.ZipFile(archive_path) as archive:
            if len(archive.infolist()) > 10000 or sum(entry.file_size for entry in archive.infolist()) > 256 * 1024 * 1024:
                raise ValueError('Snapshot exceeds the bounded recovery size')
            manifest = json.loads(archive.read('manifest.json'))
            if manifest.get('format') != 1 or manifest.get('sourceStable') is not True or manifest.get('database') != 'jinse_wine':
                raise ValueError('Snapshot format or source consistency is invalid')
            tables = manifest['tables']
            if not tables or any(not re.fullmatch(r'[A-Za-z0-9_]+', name) or not isinstance(count, int) or count < 0 for name, count in tables.items()):
                raise ValueError('Invalid snapshot table inventory')
            sql_data = archive.read('database.sql')
            if sha(sql_data) != manifest['sqlSha256']:
                raise ValueError('SQL hash does not match the authenticated manifest')
            if re.search(rb'^\s*(?:USE\s|(?:CREATE|DROP|ALTER)\s+DATABASE\s|(?:CREATE|DROP)\s+USER\s|GRANT\s)', sql_data, re.I | re.M):
                raise ValueError('SQL contains a database switch or global account statement')
            expected_entries = {'manifest.json', 'database.sql'}
            keys = set()
            for item in manifest['objects']:
                if not re.fullmatch(r'objects/[0-9]+\.bin', item['file']) or item['file'] in expected_entries or item['key'] in keys:
                    raise ValueError('Invalid or duplicated object archive entry')
                expected_entries.add(item['file'])
                keys.add(item['key'])
            if set(archive.namelist()) != expected_entries or len(archive.namelist()) != len(expected_entries):
                raise ValueError('Snapshot contains unexpected or duplicated archive entries')
            sql_path.write_bytes(sql_data)
            query('CREATE DATABASE `' + temporary_db + '` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci')
            db_created = True
            with sql_path.open('rb') as source:
                imported = subprocess.run([mysql, *db_args, '--binary-mode', '--database=' + temporary_db], env=db_env, stdin=source, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=120)
            if imported.returncode:
                raise RuntimeError('Isolated SQL import failed; private diagnostics withheld')
            actual_tables = query('SHOW TABLES', temporary_db).splitlines()
            if set(actual_tables) != set(tables):
                raise RuntimeError('Restored table names differ from the production snapshot')
            actual_counts = {name: int(query('SELECT COUNT(*) FROM `' + name + '`', temporary_db)) for name in tables}
            if actual_counts != tables:
                raise RuntimeError('Restored table row counts differ from the production snapshot')
            actual_columns = query("SELECT table_name,column_name,column_type,is_nullable FROM information_schema.columns WHERE table_schema=DATABASE() ORDER BY table_name,ordinal_position", temporary_db)
            actual_indexes = query("SELECT table_name,index_name,seq_in_index,column_name,non_unique FROM information_schema.statistics WHERE table_schema=DATABASE() ORDER BY table_name,index_name,seq_in_index", temporary_db)
            if schema_shape(actual_columns, actual_indexes) != schema_shape(manifest['columns'], manifest['indexes']):
                differences = {kind: {'sourceOnly': sorted(set(expected.splitlines()) - set(actual.splitlines()))[:10],
                                      'restoredOnly': sorted(set(actual.splitlines()) - set(expected.splitlines()))[:10]}
                               for kind, expected, actual in [('columns', manifest['columns'], actual_columns), ('indexes', manifest['indexes'], actual_indexes)]
                               if expected != actual}
                raise RuntimeError('Restored schema differs: ' + json.dumps(differences))
            storage.request('PUT', temporary_bucket)
            bucket_created = True
            total_bytes = 0
            for item in manifest['objects']:
                data = archive.read(item['file'])
                if len(data) != item['bytes'] or sha(data) != item['sha256']:
                    raise RuntimeError('Archived object hash differs')
                storage.request('PUT', temporary_bucket, item['key'], data=data, metadata=item['metadata'])
                uploaded.append(item['key'])
                tags = item.get('tags')
                if tags:
                    storage.request('PUT', temporary_bucket, item['key'], query={'tagging': ''}, data=tags.encode(), metadata={'content-type': 'application/xml'})
                restored, headers = storage.request('GET', temporary_bucket, item['key'])
                if sha(restored) != item['sha256'] or object_metadata(headers) != item['metadata']:
                    raise RuntimeError('Restored object contents or metadata differ')
                if tags and tag_values(storage.request('GET', temporary_bucket, item['key'], query={'tagging': ''})[0]) != tag_values(tags):
                    raise RuntimeError('Restored object tags differ')
                total_bytes += len(restored)
            if {item['key'] for item in storage.objects(temporary_bucket)} != keys:
                raise RuntimeError('Restored object names differ')
            current_jar = backend / 'ruoyi-admin/target/ruoyi-admin.jar'
            if not current_jar.exists():
                current_jar = backend / 'backend/ruoyi-admin.jar'
            result = {'completedAt': datetime.datetime.now().astimezone().isoformat(), 'scope': 'production data restored on a separate Windows host',
                      'archive': str(archive_path), 'archiveSha256': sha(archive_path.read_bytes()),
                      'tables': len(tables), 'rows': sum(tables.values()), 'columns': len(actual_columns.splitlines()),
                      'indexColumns': len(actual_indexes.splitlines()), 'objects': len(keys), 'objectBytes': total_bytes,
                      'allRowCountsMatched': True, 'allColumnsAndIndexesMatched': True,
                      'allObjectHashesMetadataAndTagsMatched': True, 'sourceStable': True,
                      'snapshotBackendJarSha256': manifest['backendJarSha256'], 'localBackendJarSha256': sha(current_jar.read_bytes()) if current_jar.exists() else None}
            if application_check:
                result['application'] = application_check(temporary_db, temporary_bucket, manifest, query, storage, archive_path.parent)
    finally:
        errors = []
        if bucket_created:
            try:
                for item in storage.objects(temporary_bucket):
                    storage.request('DELETE', temporary_bucket, item['key'])
                storage.request('DELETE', temporary_bucket)
            except Exception:
                errors.append('Temporary recovery bucket requires cleanup: ' + temporary_bucket)
        if db_created:
            try:
                query('DROP DATABASE `' + temporary_db + '`')
                if query("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='" + temporary_db + "'") != '0':
                    raise RuntimeError('Temporary database remains')
            except Exception:
                errors.append('Temporary recovery database requires cleanup: ' + temporary_db)
        if sql_path.exists():
            sql_path.unlink()
        if errors:
            raise RuntimeError('; '.join(errors))
    result['temporaryResourcesRemoved'] = True
    receipt_path.write_text(json.dumps(result, indent=2), encoding='utf-8')
    print(json.dumps(result), flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--archive', required=True)
    parser.add_argument('--receipt', required=True)
    parser.add_argument('--verify-application', action='store_true')
    args = parser.parse_args()
    check = None
    if args.verify_application:
        from application_recovery import verify_application
        check = verify_application
    restore(args.archive, args.receipt, check)
