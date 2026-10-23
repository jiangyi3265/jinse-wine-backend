#!/usr/bin/env bash
set -euo pipefail
umask 077
if [[ "${EUID}" -ne 0 ]]; then printf '%s\n' 'Run this installer as root.' >&2; exit 1; fi
if [[ "$#" -ne 1 || ! -f "$1" ]]; then printf '%s\n' 'Usage: install-scheduled-backup.sh recovery-public.pem' >&2; exit 1; fi
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
backup_python="${BACKUP_PYTHON:-/www/server/panel/pyenv/bin/python3}"
"$backup_python" -c 'import cryptography'
install -d -m 700 /opt/jinse-wine-backup
if [[ -f /opt/jinse-wine-backup/public.pem ]] && ! cmp -s -- "$1" /opt/jinse-wine-backup/public.pem; then
    printf '%s\n' 'Recovery key changed: migrate retained snapshots before rotating the key.' >&2
    exit 1
fi
for source in snapshot_storage.py export-production-joint-snapshot.py run-scheduled-backup.py; do
    install -m 600 -- "$script_dir/$source" "/opt/jinse-wine-backup/$source"
done
install -m 600 -- "$1" /opt/jinse-wine-backup/public.pem
"$backup_python" - "$backup_python" <<'PY'
from pathlib import Path
import sys
python = Path(sys.argv[1]).resolve()
if any(character in str(python) for character in (' ', '\n', '"')):
    raise ValueError('Python runtime path is not supported by this unit template')
Path('/etc/systemd/system/jinse-wine-joint-backup.service').write_text('''[Unit]
Description=Jinse Wine encrypted MySQL and MinIO joint snapshot
After=network.target mysql.service jinse-minio.service

[Service]
Type=oneshot
User=root
UMask=0077
WorkingDirectory=/opt/jinse-wine-backup
ExecStart=''' + str(python) + ''' /opt/jinse-wine-backup/run-scheduled-backup.py
TimeoutStartSec=360
''')
Path('/etc/systemd/system/jinse-wine-joint-backup.timer').write_text('''[Unit]
Description=Jinse Wine daily consistent joint backup at 03:40 China time

[Timer]
OnCalendar=*-*-* 03:40:00 Asia/Shanghai
RandomizedDelaySec=300
Persistent=true
Unit=jinse-wine-joint-backup.service

[Install]
WantedBy=timers.target
''')
PY
systemctl daemon-reload
systemctl enable --now jinse-wine-joint-backup.timer
systemctl list-timers jinse-wine-joint-backup.timer --no-pager
