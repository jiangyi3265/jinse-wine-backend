#!/usr/bin/env bash
# Run on the authorized Linux deployment host after placing a verified MinIO
# binary beside this script. Never prints the generated credentials.
set -euo pipefail
release_dir=$(cd -- "$(dirname -- "$0")" && pwd)
service_root=/www/server/jinse-minio
backend_root=/www/wwwroot/jinse-wine/backend
config_file="$backend_root/config/application.properties"
unit_file=/etc/systemd/system/jinse-minio.service

test "$(id -u)" = 0
test -f "$release_dir/minio-linux-amd64"
test -f "$backend_root/ruoyi-admin.jar"
if test -e "$service_root" || test -e "$unit_file" || test -e "$config_file"; then
  echo 'Existing MinIO installation or external application.properties found; inspect before changing it.' >&2
  exit 1
fi
if ss -lntH '( sport = :9000 or sport = :9001 )' | grep -q .; then
  echo 'MinIO ports are already in use.' >&2
  exit 1
fi
(cd -- "$release_dir" && sha256sum -c SHA256SUMS)
getent passwd jinse-minio >/dev/null || useradd --system --no-create-home --shell /sbin/nologin jinse-minio
install -d -m 0755 "$service_root"
install -d -m 0700 -o jinse-minio -g jinse-minio "$service_root/data"
install -m 0755 "$release_dir/minio-linux-amd64" "$service_root/minio"
"$service_root/minio" --version
python3 - "$service_root" <<'PY'
import os, pathlib, secrets, sys
root = pathlib.Path(sys.argv[1])
path = root / 'minio.env'
fd = os.open(str(path), os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
with os.fdopen(fd, 'w') as output:
    output.write('MINIO_ROOT_USER=jinse-wine-storage\n')
    output.write('MINIO_ROOT_PASSWORD=' + secrets.token_urlsafe(36) + '\n')
PY
cat > "$unit_file" <<'UNIT'
[Unit]
Description=Jinse Wine private MinIO object storage
After=network-online.target
Wants=network-online.target

[Service]
User=jinse-minio
Group=jinse-minio
EnvironmentFile=/www/server/jinse-minio/minio.env
ExecStart=/www/server/jinse-minio/minio server /www/server/jinse-minio/data --address 127.0.0.1:9000 --console-address 127.0.0.1:9001
Restart=on-failure
RestartSec=5
LimitNOFILE=65536
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/www/server/jinse-minio/data

[Install]
WantedBy=multi-user.target
UNIT
systemctl daemon-reload
systemctl enable --now jinse-minio
ready=false
for attempt in $(seq 1 30); do
  if curl --fail --silent --max-time 2 http://127.0.0.1:9000/minio/health/live >/dev/null; then
    ready=true
    break
  fi
  sleep 1
done
if test "$ready" != true; then
  echo 'MinIO did not become healthy. Java configuration was not changed.' >&2
  exit 1
fi
install -d -m 0750 -o root -g www "$backend_root/config"
python3 - "$service_root/minio.env" "$config_file" <<'PY'
import grp, os, pathlib, sys
values = dict(line.split('=', 1) for line in pathlib.Path(sys.argv[1]).read_text().splitlines() if '=' in line)
fd = os.open(sys.argv[2], os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o640)
os.fchown(fd, 0, grp.getgrnam('www').gr_gid)
with os.fdopen(fd, 'w') as output:
    output.write('ruoyi.public-url=https://jiuyekl.oksja.cn\n')
    output.write('storage.minio.enabled=true\n')
    output.write('storage.minio.endpoint=http://127.0.0.1:9000\n')
    output.write('storage.minio.access-key=' + values['MINIO_ROOT_USER'] + '\n')
    output.write('storage.minio.secret-key=' + values['MINIO_ROOT_PASSWORD'] + '\n')
    output.write('storage.minio.bucket=jinse-wine\n')
    output.write('storage.minio.seed-local-files=true\n')
PY
echo 'MinIO is healthy and enabled for boot. Java storage configuration prepared.'
echo 'Restart ruoyi-admin, then verify object hashes, public images, and a new upload.'
echo "Rollback before any new object uploads: move $config_file outside the config directory and restart ruoyi-admin."
