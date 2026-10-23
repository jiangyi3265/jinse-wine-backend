#!/usr/bin/env python3
"""Maintain encrypted joint snapshots and retain unacknowledged copies."""
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys

ROOT = Path('/www/backup/jinse-wine')
WORK = Path('/opt/jinse-wine-backup')
NAME = re.compile(r'production-joint-\d{8}-\d{6}-[a-f0-9]{8}')


def write_json(path, value):
    temporary = path.with_suffix(path.suffix + '.tmp')
    temporary.write_text(json.dumps(value, indent=2), encoding='utf-8')
    temporary.replace(path)


def acknowledged(directory, receipt):
    try:
        ack = json.loads((directory / 'offsite-ack.json').read_text())
        return (ack.get('encryptedSha256') == receipt['encryptedSha256']
                and ack.get('recoveryKeySha256') == receipt['recoveryKeySha256']
                and ack.get('decryptVerified') is True)
    except (OSError, ValueError, KeyError):
        return False


def inventory(root, key_hash):
    results = []
    for directory in root.iterdir():
        if directory.is_symlink() or not directory.is_dir() or not NAME.fullmatch(directory.name):
            continue
        try:
            receipt = json.loads((directory / 'receipt.json').read_text())
            if (receipt.get('status') != 'success' or receipt.get('recoveryKeySha256') != key_hash
                    or receipt.get('backup') != str(directory)):
                continue
            results.append({'name': directory.name, 'receipt': receipt,
                            'acknowledged': acknowledged(directory, receipt)})
        except (OSError, ValueError):
            continue
    return sorted(results, key=lambda item: item['name'], reverse=True)


def retain(root, snapshots, keep=14):
    # Old snapshots are removable only after authenticated decryption off site.
    for item in snapshots[keep:]:
        if not item['acknowledged']:
            continue
        target = root / item['name']
        if target.parent != root or target.is_symlink() or not NAME.fullmatch(target.name):
            raise ValueError('Retention target is outside the managed backup directory')
        shutil.rmtree(target)


def run():
    os.umask(0o077)
    ROOT.mkdir(mode=0o700, exist_ok=True)
    key_hash = hashlib.sha256((WORK / 'public.pem').read_bytes()).hexdigest()
    status_file = ROOT / 'scheduled-backup-status.json'
    status = json.loads(status_file.read_text()) if status_file.exists() else {}
    now = datetime.datetime.now(datetime.timezone.utc)
    status.update(lastAttemptAt=now.isoformat(), status='running')
    write_json(status_file, status)
    output = ROOT / ('production-joint-' + now.strftime('%Y%m%d-%H%M%S') + '-' + secrets.token_hex(4))
    try:
        if shutil.disk_usage(ROOT).free < 512 * 1024 * 1024:
            raise RuntimeError('Insufficient backup disk space')
        with (WORK / 'last-export.log').open('wb') as log:
            result = subprocess.run([sys.executable, str(WORK / 'export-production-joint-snapshot.py'),
                                     '--output', str(output), '--public-key', str(WORK / 'public.pem')],
                                    stdout=log, stderr=subprocess.STDOUT, timeout=300)
        if result.returncode:
            raise RuntimeError('Joint export failed; inspect the restricted server log')
        receipt = json.loads((output / 'receipt.json').read_text())
        if receipt.get('status') != 'success' or not receipt.get('javaHealthy') or not receipt.get('sourceStable'):
            raise RuntimeError('Joint snapshot did not pass consistency and health checks')
        receipt.update(recoveryKeySha256=key_hash, scheduled=True, createdAt=now.isoformat())
        write_json(output / 'receipt.json', receipt)
        snapshots = inventory(ROOT, key_hash)
        retain(ROOT, snapshots)
        snapshots = inventory(ROOT, key_hash)
        write_json(ROOT / 'scheduled-catalog.json', {'format': 1, 'recoveryKeySha256': key_hash,
                   'updatedAt': now.isoformat(), 'snapshots': snapshots})
        status.update(status='success', lastSuccessAt=now.isoformat(), backup=str(output),
                      encryptedSha256=receipt['encryptedSha256'], consecutiveFailures=0,
                      retained=len(snapshots), unacknowledged=sum(not x['acknowledged'] for x in snapshots))
        status.pop('error', None)
    except Exception as error:
        status.update(status='failed', consecutiveFailures=status.get('consecutiveFailures', 0) + 1,
                      error=type(error).__name__ + ': inspect restricted last-export.log')
        raise
    finally:
        # Failed exports must not retain an unencrypted SQL/ZIP alongside ciphertext.
        for name in ('database.sql', 'snapshot.zip'):
            path = output / name
            if path.is_file() and not path.is_symlink():
                path.unlink()
        write_json(status_file, status)
    print(json.dumps(status), flush=True)


if __name__ == '__main__':
    import fcntl
    with (WORK / 'scheduler.lock').open('w') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        run()
