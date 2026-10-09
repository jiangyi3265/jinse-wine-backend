#!/usr/bin/env python3
"""Read-only migration audit on the Linux host; credentials stay on the host."""
import argparse
import datetime
import hashlib
import hmac
import json
import subprocess
from pathlib import Path
from urllib.parse import quote
from urllib.request import Request, urlopen
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--backend', default='/www/wwwroot/jinse-wine/backend')
parser.add_argument('--uploads', default='/www/wwwroot/jinse-wine/uploads')
parser.add_argument('--public-base', default='https://jiuyekl.oksja.cn')
parser.add_argument('--local-base', default='http://127.0.0.1:8080')
parser.add_argument('--extra-key', action='append', default=[])
args = parser.parse_args()
settings = dict(line.split('=', 1) for line in (Path(args.backend) / 'config/application.properties').read_text().splitlines()
                if '=' in line and not line.startswith('#'))
assert settings['storage.minio.enabled'] == 'true', 'MinIO is not enabled in external config'
assert settings['storage.minio.endpoint'] == 'http://127.0.0.1:9000', 'Unexpected MinIO endpoint'
key_id = settings['storage.minio.access-key']
secret = settings['storage.minio.secret-key']
bucket = settings['storage.minio.bucket']
pid = subprocess.check_output(['systemctl', 'show', '-p', 'MainPID', '--value', 'marathon.service'], text=True).strip()
runtime = dict(line.split('=', 1) for line in Path('/proc/' + pid + '/environ').read_bytes().decode().split('\0') if '=' in line)
for variable, property_name in {'MINIO_ENABLED': 'storage.minio.enabled', 'MINIO_ENDPOINT': 'storage.minio.endpoint',
                               'MINIO_ACCESS_KEY': 'storage.minio.access-key', 'MINIO_SECRET_KEY': 'storage.minio.secret-key',
                               'MINIO_BUCKET': 'storage.minio.bucket'}.items():
    if runtime.get(variable) != settings.get(property_name):
        raise RuntimeError('Running Java does not use the expected MinIO configuration; values withheld')

def sha(value):
    return hashlib.sha256(value).hexdigest()

def mac(key, value):
    return hmac.new(key, value.encode(), hashlib.sha256).digest()

def object_bytes(key):
    timestamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    date = timestamp[:8]
    path = '/' + quote(bucket, safe='') + '/' + quote(key, safe='/')
    signed_headers = 'host;x-amz-content-sha256;x-amz-date'
    headers_text = 'host:127.0.0.1:9000\nx-amz-content-sha256:' + sha(b'') + '\nx-amz-date:' + timestamp + '\n'
    canonical = '\n'.join(['GET', path, '', headers_text, signed_headers, sha(b'')])
    scope = date + '/us-east-1/s3/aws4_request'
    signing_key = mac(mac(mac(mac(('AWS4' + secret).encode(), date), 'us-east-1'), 's3'), 'aws4_request')
    signature = mac(signing_key, 'AWS4-HMAC-SHA256\n' + timestamp + '\n' + scope + '\n' + sha(canonical.encode())).hex()
    headers = {'x-amz-date': timestamp, 'x-amz-content-sha256': sha(b''),
               'Authorization': 'AWS4-HMAC-SHA256 Credential=' + key_id + '/' + scope + ', SignedHeaders=' + signed_headers + ', Signature=' + signature}
    with urlopen(Request('http://127.0.0.1:9000' + path, headers=headers), timeout=15) as response:
        return response.read()

def served_bytes(base, key):
    with urlopen(base.rstrip('/') + '/' + quote(key, safe='/'), timeout=15) as response:
        return response.read()

verified = []

def check(key, expected=None):
    if not key.startswith(('mini-assets/', 'profile/')) or '..' in key or '\\' in key:
        raise ValueError('Invalid media key')
    stored = object_bytes(key)
    if expected is not None and stored != expected:
        raise RuntimeError('Migrated object differs from original: ' + key)
    if served_bytes(args.local_base, key) != stored:
        raise RuntimeError('Java media differs from MinIO: ' + key)
    verified.append({'key': key, 'bytes': len(stored), 'sha256': sha(stored)})

with zipfile.ZipFile(Path(args.backend) / 'ruoyi-admin.jar') as archive:
    prefix = 'BOOT-INF/classes/static/mini-assets/'
    for name in archive.namelist():
        if name.startswith(prefix) and not name.endswith('/'):
            check('mini-assets/' + name[len(prefix):], archive.read(name))
upload_root = Path(args.uploads)
if upload_root.exists():
    for path in upload_root.rglob('*'):
        if path.is_file() and not path.is_symlink():
            check('profile/' + path.relative_to(upload_root).as_posix(), path.read_bytes())
for key in args.extra_key:
    check(key)
if not verified:
    raise RuntimeError('No objects checked')
public_keys = list(dict.fromkeys([verified[0]['key']] + args.extra_key))
for key in public_keys:
    if served_bytes(args.public_base, key) != object_bytes(key):
        raise RuntimeError('Public media differs from MinIO: ' + key)
print(json.dumps({'bucket': bucket, 'matchedObjects': len(verified), 'publicKeys': public_keys, 'objects': verified}, indent=2))
