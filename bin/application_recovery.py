"""Start recovered artifacts with isolated data/cache and disabled integrations."""
from contextlib import ExitStack
import functools
import hashlib
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import socket
import subprocess
import threading
import time
from urllib.error import HTTPError
from urllib.parse import quote, urlsplit
from urllib.request import Request, urlopen
import zipfile


def free_port():
    with socket.socket() as probe:
        probe.bind(('127.0.0.1', 0))
        return probe.getsockname()[1]


def stop_process(process):
    if process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=15)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)


def isolated_environment(database, username, password, bucket, storage, work):
    if not re.fullmatch(r'jinse_offsite_[a-f0-9]{32}', database) or not re.fullmatch(r'jinse-offsite-[a-f0-9]{32}', bucket):
        raise ValueError('Recovery application requires isolated database and bucket names')
    preserved = ('PATH', 'Path', 'SYSTEMROOT', 'WINDIR', 'TEMP', 'TMP', 'COMSPEC', 'USERPROFILE', 'APPDATA', 'LOCALAPPDATA', 'JAVA_HOME')
    env = {name: os.environ[name] for name in preserved if name in os.environ}
    env.update(DB_URL='jdbc:mysql://127.0.0.1:3306/' + database + '?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true',
               DB_USERNAME=username, DB_PASSWORD=password, JWT_SECRET=secrets.token_hex(32),
               APP_ENV='recovery', APP_SCHEDULING_ENABLED='false', MINI_LOCAL_LOGIN_ENABLED='false',
               MINI_WECHAT_PAY_ENABLED='false', MINI_WECHAT_TRANSFER_ENABLED='false', WECHAT_LOGISTICS_ENABLED='false',
               MINIO_ENABLED='true', MINIO_ENDPOINT=storage.endpoint, MINIO_ACCESS_KEY=storage.access,
               MINIO_SECRET_KEY=storage.secret, MINIO_BUCKET=bucket, MINIO_SEED_LOCAL_FILES='false',
               UPLOAD_PATH=str(work / 'uploads'), APP_LOG_LEVEL='warn', SPRING_DEVTOOLS_RESTART_ENABLED='false')
    return env


class Gateway(SimpleHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def route_api(self):
        path = self.path
        if not path.startswith('/prod-api/'):
            return False
        body = self.rfile.read(int(self.headers.get('Content-Length', '0'))) if self.command == 'POST' else None
        headers = {key: value for key, value in self.headers.items() if key.lower() in ('authorization', 'content-type')}
        request = Request(self.server.backend + path[len('/prod-api'):], data=body, headers=headers, method=self.command)
        try:
            response = urlopen(request, timeout=20)
        except HTTPError as error:
            response = error
        with response:
            data = response.read()
            self.send_response(response.status)
            self.send_header('Content-Type', response.headers.get('Content-Type', 'application/octet-stream'))
            self.send_header('Content-Length', str(len(data)))
            self.end_headers()
            self.wfile.write(data)
        return True

    def do_GET(self):
        if self.route_api():
            return
        requested = Path(self.translate_path(self.path))
        if not requested.is_file() and not requested.is_dir():
            self.path = '/index.html'
        super().do_GET()

    def do_POST(self):
        if not self.route_api():
            self.send_error(405)


def static_gateway(directory, backend):
    server = ThreadingHTTPServer(('127.0.0.1', 0), functools.partial(Gateway, directory=str(directory)))
    server.backend = backend
    threading.Thread(target=server.serve_forever, daemon=True).start()
    return server


def request_json(base, path, token=None, payload=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    request = Request(base + path, headers=headers, data=json.dumps(payload).encode() if payload is not None else None)
    with urlopen(request, timeout=20) as response:
        return json.load(response)


def verify_application(database, bucket, manifest, query, storage, backup_directory):
    backend = Path(__file__).resolve().parents[1]
    workspace = backend.parent
    work = backup_directory / ('application-' + secrets.token_hex(8))
    work.mkdir(mode=0o700)
    jar_default = backend / 'ruoyi-admin/target/ruoyi-admin.jar'
    if not jar_default.exists():
        jar_default = backend / 'backend/ruoyi-admin.jar'
    jar = Path(os.environ.get('RECOVERY_JAR', jar_default)).resolve()
    admin_default = workspace / 'jinse-wine-admin/dist'
    h5_default = workspace / 'jinse-wine-app/unpackage/dist/build/h5'
    admin = Path(os.environ.get('RECOVERY_ADMIN_BUILD', admin_default if admin_default.exists() else backend / 'admin')).resolve()
    h5 = Path(os.environ.get('RECOVERY_H5_BUILD', h5_default if h5_default.exists() else backend / 'h5')).resolve()
    templates = backend / 'config-templates/examples'
    java = Path(os.environ['JAVA_HOME']) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')
    redis = Path(os.environ['RECOVERY_REDIS_SERVER'])
    node = os.environ.get('RECOVERY_NODE') or shutil.which('node')
    playwright = os.environ['RECOVERY_PLAYWRIGHT_MODULE']
    if not node or any(not path.is_file() for path in (jar, java, redis, admin / 'index.html', h5 / 'index.html')):
        raise ValueError('Recovery runtime or independent build artifacts are missing')
    with zipfile.ZipFile(jar) as archive:
        if 'BOOT-INF/classes/com/ruoyi/web/core/config/BusinessSchedulingConfig.class' not in archive.namelist():
            raise ValueError('Recovery JAR does not contain the scheduling switch')
        if b'Lorg/springframework/scheduling/annotation/EnableScheduling;' in archive.read('BOOT-INF/classes/com/ruoyi/RuoYiApplication.class'):
            raise ValueError('Recovery JAR unconditionally enables background tasks')
        if any('ruoyi-quartz' in name for name in archive.namelist()):
            raise ValueError('Recovery exercise does not permit executable Quartz jobs')
    for name in ('application.yml', 'application-druid.yml'):
        shutil.copy2(templates / name, work / name)
    username = 'jinse_dr_' + secrets.token_hex(8)
    password = secrets.token_hex(24)
    user_created = False
    api_port, redis_port = free_port(), free_port()
    base = 'http://127.0.0.1:' + str(api_port)
    checks = []
    creation_flags = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0
    with ExitStack() as resources:
        try:
            query("CREATE USER '" + username + "'@'127.0.0.1' IDENTIFIED BY '" + password + "'")
            user_created = True
            query("GRANT ALL ON `" + database + "`.* TO '" + username + "'@'127.0.0.1'")
            env = isolated_environment(database, username, password, bucket, storage, work)
            redis_log = resources.enter_context((work / 'redis.log').open('wb'))
            redis_process = subprocess.Popen([str(redis), '--port', str(redis_port), '--bind', '127.0.0.1', '--save', '', '--appendonly', 'no', '--dir', str(work)],
                                             cwd=work, stdout=redis_log, stderr=subprocess.STDOUT, creationflags=creation_flags)
            resources.callback(stop_process, redis_process)
            java_log = resources.enter_context((work / 'java.log').open('wb'))
            java_process = subprocess.Popen([str(java), '-Xms64m', '-Xmx384m', '-XX:MaxMetaspaceSize=192m',
                                            '-XX:ReservedCodeCacheSize=64m', '-XX:ActiveProcessorCount=2', '-jar', str(jar),
                                            '--spring.config.location=' + ','.join((work / name).as_uri() for name in ('application.yml', 'application-druid.yml')),
                                            '--server.address=127.0.0.1', '--server.port=' + str(api_port),
                                            '--server.tomcat.threads.min-spare=5', '--server.tomcat.threads.max=32',
                                            '--spring.redis.host=127.0.0.1', '--spring.redis.port=' + str(redis_port),
                                            '--spring.quartz.auto-startup=false', '--app.scheduling.enabled=false',
                                            '--ruoyi.public-url='],
                                           env=env, cwd=work, stdout=java_log, stderr=subprocess.STDOUT, creationflags=creation_flags)
            resources.callback(stop_process, java_process)
            deadline = time.monotonic() + 90
            while time.monotonic() < deadline:
                if java_process.poll() is not None or redis_process.poll() is not None:
                    raise RuntimeError('Recovery service exited; inspect private application logs')
                try:
                    catalog = request_json(base, '/mini/products')
                    if catalog.get('code') == 0:
                        break
                except (OSError, ValueError):
                    pass
                time.sleep(0.5)
            else:
                raise RuntimeError('Recovery Java did not become ready within 90 seconds')
            for path, table, condition in (('/mini/products', 'biz_product', "status='0'"), ('/mini/stores', 'biz_store', "status='0'")):
                body = request_json(base, path)
                count = int(query('SELECT COUNT(*) FROM ' + table + ' WHERE ' + condition, database))
                if body.get('code') != 0 or len(body['data']) != count:
                    raise RuntimeError('Recovered public catalog differs from restored database')
                checks.append({'endpoint': path, 'rows': count, 'matchesRecoveredDatabase': True})
            login = request_json(base, '/login', payload={'username': os.environ.get('RECOVERY_ADMIN_USERNAME', 'admin'), 'password': os.environ['RECOVERY_ADMIN_PASSWORD'], 'code': ''})
            if login.get('code') != 200 or not login.get('token'):
                raise RuntimeError('Recovered administrator could not sign in')
            token = login['token']
            for path in ('/getInfo', '/getRouters', '/business/order/list?record=true&pageNum=1&pageSize=2',
                         '/business/member/list?record=true&pageNum=1&pageSize=2', '/business/extension/dashboard',
                         '/business/extension/aftersales?pageNum=1&pageSize=2'):
                if request_json(base, path, token).get('code') != 200:
                    raise RuntimeError('Recovered administrator endpoint failed: ' + path)
                checks.append({'endpoint': path, 'success': True})
            for kind in ('orders', 'refunds', 'returns', 'commissions', 'settlements', 'lottery', 'redemptions', 'rankingRewards', 'videoRefunds'):
                path = '/business/extension/reports/' + kind
                if request_json(base, path + '?pageNum=1&pageSize=2', token).get('code') != 200:
                    raise RuntimeError('Recovered financial report list failed: ' + kind)
                with urlopen(Request(base + path + '/export', headers={'Authorization': 'Bearer ' + token}), timeout=20) as response:
                    if not response.headers.get('Content-Type', '').startswith('text/csv') or not response.read().decode('utf-8-sig').strip():
                        raise RuntimeError('Recovered financial export failed: ' + kind)
                checks.append({'report': kind, 'listAndExport': True})
            member = int(query("SELECT member_id FROM biz_member WHERE status='0' ORDER BY (SELECT COUNT(*) FROM biz_order o WHERE o.member_id=biz_member.member_id) DESC,member_id LIMIT 1", database))
            member_token = secrets.token_hex(32)
            query("INSERT INTO biz_member_session(token_hash,member_id,expires_at) VALUES('" + hashlib.sha256(member_token.encode()).hexdigest() + "'," + str(member) + ",DATE_ADD(NOW(),INTERVAL 10 MINUTE))", database)
            for path in ('/mini/me', '/mini/orders?pageNum=1&pageSize=2', '/mini/draws?pageNum=1&pageSize=2', '/mini/points/records?pageNum=1&pageSize=2'):
                if request_json(base, path, member_token).get('code') != 0:
                    raise RuntimeError('Recovered member endpoint failed: ' + path)
                checks.append({'endpoint': path, 'success': True})
            media = []
            for item in manifest['objects']:
                if not item['key'].startswith(('profile/', 'mini-assets/')) or not item['key'].lower().endswith(('.png', '.jpg', '.jpeg', '.webp', '.gif')):
                    continue
                with urlopen(base + '/' + quote(item['key'], safe='/'), timeout=15) as response:
                    if hashlib.sha256(response.read()).hexdigest() != item['sha256']:
                        raise RuntimeError('Recovered Java image does not match restored MinIO')
                media.append(item['key'])
            admin_server, h5_server = static_gateway(admin, base), static_gateway(h5, base)
            for server in (admin_server, h5_server):
                resources.callback(server.server_close)
                resources.callback(server.shutdown)
            browser_env = dict(os.environ, RECOVERY_API=base, RECOVERY_ADMIN_ORIGIN='http://127.0.0.1:' + str(admin_server.server_port),
                               RECOVERY_H5_ORIGIN='http://127.0.0.1:' + str(h5_server.server_port), RECOVERY_ADMIN_TOKEN=token,
                               RECOVERY_MEMBER_TOKEN=member_token, RECOVERY_PLAYWRIGHT_MODULE=playwright, RECOVERY_UI_OUTPUT=str(work))
            browser = subprocess.run([node, str(backend / 'bin/verify-recovered-ui.mjs')], env=browser_env, capture_output=True, timeout=150, creationflags=creation_flags)
            if browser.returncode:
                (work / 'browser.log').write_bytes(browser.stdout + browser.stderr)
                raise RuntimeError('Recovered browser workflow failed; inspect private browser.log')
            ui = json.loads(browser.stdout.decode())
            result = {'javaStarted': True, 'redisIsolated': True, 'databaseUserRestricted': True,
                      'backgroundJobsDisabled': True, 'externalWechatCredentialsAbsent': True,
                      'adminSignedIn': True, 'apiChecks': checks, 'imageHashesThroughJavaMatched': len(media),
                      'jarSha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
                      'adminIndexSha256': hashlib.sha256((admin / 'index.html').read_bytes()).hexdigest(),
                      'h5IndexSha256': hashlib.sha256((h5 / 'index.html').read_bytes()).hexdigest(),
                      'ui': ui, 'privateLogs': str(work)}
        finally:
            try:
                resources.close()
            finally:
                if user_created:
                    query("DROP USER '" + username + "'@'127.0.0.1'")
    result['temporaryProcessesAndDbUserRemoved'] = True
    return result
