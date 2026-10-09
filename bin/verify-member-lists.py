import hashlib
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import urllib.parse
import urllib.request


if os.name == 'nt':
    environment = dict(os.environ)
    base = 'http://127.0.0.1:8080'
else:
    pid = subprocess.check_output(['systemctl', 'show', '-p', 'MainPID', '--value', 'marathon.service'], text=True).strip()
    environment = dict(entry.split('=', 1) for entry in Path('/proc/' + pid + '/environ').read_bytes().decode().split('\0') if '=' in entry)
    base = 'https://jiuyekl.oksja.cn'
url = environment['DB_URL']
database = urllib.parse.urlparse(url[5:] if url.startswith('jdbc:') else url)
if database.path.lstrip('/') != 'jinse_wine':
    raise RuntimeError('Unexpected verification database')
db_env = dict(os.environ, MYSQL_PWD=environment['DB_PASSWORD'])
db_args = [shutil.which('mysql') or '/www/server/mysql/bin/mysql', '--host=' + (database.hostname or '127.0.0.1'),
           '--port=' + str(database.port or 3306), '--user=' + environment['DB_USERNAME'],
           '--database=jinse_wine', '--batch', '--skip-column-names']


def sql(statement):
    return subprocess.check_output(db_args + ['--execute=' + statement], env=db_env, text=True, encoding='utf-8').strip()


sessions = {}


def session(member):
    if member not in sessions:
        token = secrets.token_hex(32)
        digest = hashlib.sha256(token.encode()).hexdigest()
        sql("INSERT INTO biz_member_session(token_hash,member_id,expires_at) VALUES('" + digest + "'," + str(member) + ",DATE_ADD(NOW(),INTERVAL 5 MINUTE))")
        sessions[member] = (token, digest)
    return sessions[member][0]


def get(path, member):
    request = urllib.request.Request(base + path, headers={'Authorization': 'Bearer ' + session(member)})
    with urllib.request.urlopen(request, timeout=15) as response:
        return json.load(response)


checks = []
member = int(sql("SELECT member_id FROM biz_member WHERE status='0' ORDER BY (SELECT COUNT(*) FROM biz_lottery_record r WHERE r.member_id=biz_member.member_id) DESC,member_id LIMIT 1"))
staff = sql("SELECT m.member_id FROM biz_member m JOIN biz_store s ON s.store_id=m.staff_store_id AND s.status='0' WHERE m.status='0' ORDER BY m.member_id LIMIT 1")
specs = [
    ('/mini/orders', 'biz_order', 'order_no', 'order_id', 'member_id', member, ''),
    ('/mini/draws', 'biz_lottery_record', 'record_id', 'record_id', 'member_id', member, ''),
    ('/mini/points/records', 'biz_point_record', 'point_record_id', 'point_record_id', 'member_id', member, ''),
    ('/mini/rewards', 'biz_reward', 'reward_id', 'reward_id', 'member_id', member, " AND reward_type<>'cash'"),
    ('/mini/rewards/cash', 'biz_reward', 'reward_id', 'reward_id', 'member_id', member, " AND reward_type='cash'"),
    ('/mini/promotion/records', 'biz_promotion_record', 'promotion_record_id', 'promotion_record_id', 'member_id', member, ''),
]
if staff:
    staff = int(staff)
    store = int(sql('SELECT staff_store_id FROM biz_member WHERE member_id=' + str(staff)))
    specs += [
        ('/mini/staff/redemptions', 'biz_redemption', 'redemption_id', 'redemption_id', 'verifier_member_id', staff, ''),
        ('/mini/staff/orders', 'biz_order_pickup_redemption', 'pickup_redemption_id', 'pickup_redemption_id', 'verifier_member_id', staff, ' AND store_id=' + str(store)),
    ]
try:
    for path, table, identifier, ordering, owner, principal, restriction in specs:
        first = get(path + '?pageNum=1&pageSize=2', principal)
        second = get(path + '?pageNum=2&pageSize=2', principal)
        legacy = get(path, principal)
        assert first['code'] == second['code'] == legacy['code'] == 0, (path, first.get('message'))
        where = owner + '=' + str(principal) + restriction
        expected = int(sql('SELECT COUNT(*) FROM ' + table + ' WHERE ' + where))
        ids = sql('SELECT ' + identifier + ' FROM ' + table + ' WHERE ' + where + ' ORDER BY ' + ordering + ' DESC LIMIT 4').splitlines()
        assert first['data']['total'] == second['data']['total'] == expected, path
        assert first['data']['pageNum'] == 1 and second['data']['pageNum'] == 2, path
        assert first['data']['pageSize'] == second['data']['pageSize'] == 2, path
        assert [str(row['id']) for row in first['data']['rows']] == ids[:2], path
        assert [str(row['id']) for row in second['data']['rows']] == ids[2:4], path
        assert isinstance(legacy['data'], list) and len(legacy['data']) == expected, path
        assert get(path + '?pageSize=101', principal)['code'] == 400, path
        checks.append({'endpoint': path, 'total': expected, 'twoPagesAndDescendingIdsMatchDatabase': True, 'legacyArrayComplete': True, 'excessiveSizeRejected': True})
    for path, table, statuses, restriction in [
        ('/mini/orders', 'biz_order', ['pending', 'paid', 'receiving', 'complete', 'cancelled', 'refunded'], ''),
        ('/mini/rewards', 'biz_reward', ['pending', 'redeemed', 'closed'], " AND reward_type<>'cash'"),
    ]:
        for status in statuses:
            response = get(path + '?pageNum=1&pageSize=2&status=' + status, member)
            assert response['code'] == 0, (path, status)
            condition = "status IN ('shipping','receiving')" if status == 'receiving' else "status='" + status + "'"
            count = int(sql('SELECT COUNT(*) FROM ' + table + ' WHERE member_id=' + str(member) + restriction + ' AND ' + condition))
            assert response['data']['total'] == count, (path, status)
            assert all(row['status'] in ('shipping', 'receiving') if status == 'receiving' else row['status'] == status for row in response['data']['rows']), (path, status)
        assert get(path + '?pageNum=1&status=invalid', member)['code'] == 400
    ordinary = sql("SELECT member_id FROM biz_member WHERE status='0' AND staff_store_id IS NULL LIMIT 1")
    if ordinary:
        for path in ['/mini/staff/redemptions', '/mini/staff/orders']:
            assert get(path + '?pageNum=1', int(ordinary))['code'] == 403
    report = {'status': 'passed', 'base': base, 'checks': checks, 'orderAndAwardFiltersMatchDatabase': True,
              'ordinaryMemberDeniedStaffHistory': bool(ordinary), 'activeStaffHistoryChecked': bool(staff)}
finally:
    for _, digest in sessions.values():
        sql("DELETE FROM biz_member_session WHERE token_hash='" + digest + "'")
        assert sql("SELECT COUNT(*) FROM biz_member_session WHERE token_hash='" + digest + "'") == '0'

report['temporarySessionsRemoved'] = True
print('MEMBER_LISTS_VERIFIED ' + json.dumps(report))
