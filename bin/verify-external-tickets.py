"""Local MySQL/HTTP regression only. Original ticket fixtures are not valid admission tickets."""
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta
import base64
import hashlib
import json
import os
from pathlib import Path
import subprocess
import urllib.request
import urllib.error
import uuid

root = Path(__file__).resolve().parents[2]
out = root / '.tools' / 'external-ticket-local'
out.mkdir(exist_ok=True)
assert os.environ.get('DB_PASSWORD') and os.environ.get('DB_USERNAME')
mysql = 'D:/zhangq/mysql/bin/mysql.exe'
db_env = dict(os.environ, MYSQL_PWD=os.environ['DB_PASSWORD'])
prefix = 'ticket-regression-' + uuid.uuid4().hex[:12]
members, activities, checks = [], [], []
admin = ''

def sql(query):
    result = subprocess.run([mysql, '--protocol=TCP', '--host=127.0.0.1', '--port=3306',
        '--user=' + os.environ['DB_USERNAME'], '--default-character-set=utf8mb4', '--batch',
        '--skip-column-names', 'jinse_wine'], input=query.encode(), capture_output=True, env=db_env, timeout=30)
    if result.returncode:
        raise RuntimeError(result.stderr.decode(errors='replace')[:300])
    return result.stdout.decode().strip()

def request(path, body=None, token='', key='', method=None, binary=False):
    headers = {'Authorization': 'Bearer ' + token}
    if isinstance(body, tuple):
        payload, mime = body; headers['Content-Type'] = mime
    else:
        payload = None if body is None else json.dumps(body).encode()
        headers['Content-Type'] = 'application/json'
    if key: headers['Idempotency-Key'] = key
    req = urllib.request.Request('http://127.0.0.1:8080' + path, data=payload,
        headers=headers, method=method or ('GET' if payload is None else 'POST'))
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            if binary: return response.read(), dict(response.headers)
            return json.load(response)
    except urllib.error.HTTPError as error:
        return json.load(error)

def png(name):
    path = out / (name + '.png')
    subprocess.run(['D:/software/work/nodejs/node.exe', str(root / 'jinse-wine-app/scripts/ticket-test-qr.mjs'),
        'https://ticket.invalid/local-test/' + name, str(path)], check=True, capture_output=True, timeout=15)
    return path.read_bytes()

def upload(pid, no, image, vendor='本地测试售票方', valid=None):
    boundary = 'ticket-' + uuid.uuid4().hex
    fields = {'prizeId': str(pid), 'providerName': vendor, 'externalNo': no,
        'validUntil': valid or (datetime.now() + timedelta(days=30)).strftime('%Y-%m-%d %H:%M:%S')}
    body = b''
    for name, value in fields.items():
        body += (f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}\r\n').encode()
    body += (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="ticket.png"\r\nContent-Type: image/png\r\n\r\n').encode()
    body += image + f'\r\n--{boundary}--\r\n'.encode()
    return request('/business/lottery/tickets', (body, 'multipart/form-data; boundary=' + boundary), admin)

def member():
    mid = int(sql(f"INSERT INTO biz_member(wechat_openid,nickname,status) VALUES('{prefix}-{len(members)}','门票回归会员','0'); SELECT LAST_INSERT_ID();"))
    members.append(mid)
    token = uuid.uuid4().hex + uuid.uuid4().hex
    digest = hashlib.sha256(token.encode()).hexdigest()
    sql(f"INSERT INTO biz_member_session(token_hash,member_id,expires_at) VALUES('{digest}',{mid},DATE_ADD(NOW(),INTERVAL 30 MINUTE));")
    return mid, token

def activity(name, probability=10000):
    code = prefix + '-' + str(len(activities))
    aid = int(sql(f"INSERT INTO biz_lottery_activity(activity_code,activity_name,daily_limit,total_limit,rule_json,end_time) VALUES('{code}','{name}',100,1000,'{{\"rewardValidityDays\":7}}',DATE_ADD(NOW(),INTERVAL 10 DAY)); SELECT LAST_INSERT_ID();"))
    activities.append(aid)
    result = request('/business/extension/lottery/prizes', {'activityCode': code,
        'prizeName': name, 'prizeType': 'ticket', 'probabilityBp': probability, 'stock': 999, 'status': '0'}, admin)
    assert result['code'] == 200, result
    pid = int(sql(f'SELECT prize_id FROM biz_lottery_prize WHERE activity_id={aid}'))
    assert sql(f'SELECT stock FROM biz_lottery_prize WHERE prize_id={pid}') == '-1'
    return aid, pid, code

def generate(code, count, key=None):
    return request('/business/extension/lottery/codes', {'activityCode': code, 'count': count,
        'requestId': key or uuid.uuid4().hex}, admin)

def draw(code, token, key=None):
    return request('/mini/draws', {'code': code}, token, key or uuid.uuid4().hex)

def passed(label):
    checks.append(label); print('PASS: ' + label, flush=True)

try:
    login = request('/login', {'username': 'admin', 'password': 'admin123', 'code': ''})
    assert login['code'] == 200, 'Local admin login failed'
    admin = login['token']
    mid, token = member(); other_mid, other = member()
    aid, pid, code = activity('门票回归原始奖项')
    image1, image2, image3 = png(prefix + '-1'), png(prefix + '-2'), png(prefix + '-3')
    assert upload(pid, prefix + '-1', image1)['code'] == 200
    assert upload(pid, prefix + '-2', image2)['code'] == 200
    passed('Admin can upload independent original ticket images')
    assert upload(pid, prefix + '-duplicate-image', image1)['code'] == 409
    assert upload(pid, prefix + '-1', image3)['code'] == 409
    assert upload(pid, prefix + '-expired', image3, valid='2000-01-01 00:00:00')['code'] == 400
    assert upload(pid, prefix + '-invalid', b'not an image')['code'] == 400
    assert request('/business/lottery/tickets?prizeId=' + str(pid))['code'] != 200
    passed('Duplicate ticket identity/image, expired or invalid uploads and anonymous inventory access rejected')
    key = uuid.uuid4().hex
    batch = generate(code, 3, key)
    assert batch['code'] == 200, batch
    assert batch['data']['winCount'] == 2
    codes = batch['data']['codes']
    assert [c['resultType'] for c in codes].count('ticket') == 2
    assert 'qrCode' not in json.dumps(batch) and 'ticket_id' not in json.dumps(batch)
    assert sql(f"SELECT COUNT(DISTINCT code_id) FROM biz_external_ticket WHERE prize_id={pid} AND status='reserved'") == '2'
    assert generate(code, 3, key)['data'] == batch['data']
    passed('Fixed outcomes reserve two distinct tickets; stock exhaustion yields no win; batch replay does not reserve twice')
    ticket_id = int(sql(f'SELECT MIN(ticket_id) FROM biz_external_ticket WHERE prize_id={pid}'))
    assert request(f'/business/lottery/tickets/{ticket_id}/disable', {}, admin)['code'] == 409
    sql(f"UPDATE biz_lottery_prize SET prize_name='门票配置修改后名称',probability_bp=0,status='1' WHERE prize_id={pid}")
    win = next(c['code'] for c in codes if c['resultType'] == 'ticket')
    key = uuid.uuid4().hex
    result = draw(win, token, key)
    assert result['code'] == 0, result
    reward = result['data']; rid = int(reward['id'])
    assert reward['kind'] == 'ticket' and reward['name'] == '门票回归原始奖项'
    assert reward['ticketNo'].startswith(prefix) and reward['redemptionSynced'] is False
    assert draw(win, token, key)['data']['id'] == str(rid)
    assert draw(win, other)['code'] == 409
    assert sql(f'SELECT COUNT(*) FROM biz_reward WHERE member_id={mid}') == '1'
    assert sql(f"SELECT status FROM biz_external_ticket WHERE reward_id={rid}") == 'issued'
    passed('Winner gets original ticket snapshot; idempotent draw does not issue twice; other member cannot claim same code')
    qr = request(f'/mini/rewards/{rid}/ticket-code', token=token)
    assert qr['code'] == 0, qr
    original = base64.b64decode(qr['data']['qrCode'].split(',', 1)[1])
    expected = image1 if qr['data']['ticketNo'].endswith('-1') else image2
    assert original == expected
    assert request(f'/mini/rewards/{rid}/ticket-code', token=other)['code'] == 404
    assert request(f'/mini/rewards/{rid}/ticket-code')['code'] == 401
    content, headers = request(f'/business/lottery/tickets/{ticket_id}/image', token=admin, binary=True)
    assert content == image1 and 'no-store' in headers.get('Cache-Control', '')
    assert request(f'/mini/rewards/{rid}/redeem-code', token=token)['code'] != 0
    passed('Original QR bytes preserved; only owner can view; admin image is no-store; no local redemption QR generated')
    rewards = request('/mini/rewards?pageNum=1&pageSize=20', token=token)
    assert rewards['data']['rows'][0]['ticketNo'] == reward['ticketNo']
    assert 'qrCode' not in json.dumps(rewards)
    inventory = request(f'/business/lottery/tickets?prizeId={pid}&status=issued&pageNum=1&pageSize=20', token=admin)
    assert inventory['data']['total'] == 1
    sql(f'UPDATE biz_reward SET expires_at=DATE_SUB(NOW(),INTERVAL 1 MINUTE) WHERE reward_id={rid}')
    assert request(f'/mini/rewards/{rid}/ticket-code', token=token)['code'] == 404
    assert request('/mini/rewards?status=closed&pageNum=1&pageSize=20', token=token)['data']['total'] == 1
    assert sql(f"SELECT status FROM biz_external_ticket WHERE reward_id={rid}") == 'issued'
    passed('Issued-ticket inventory and award lists agree; expired rewards hide the image and never recycle an issued ticket')
    aid2, pid2, code2 = activity('并发门票奖项')
    assert upload(pid2, prefix + '-3', image3)['code'] == 200
    with ThreadPoolExecutor(max_workers=2) as pool:
        batches = list(pool.map(lambda _: generate(code2, 1), range(2)))
    assert sum(b['data']['winCount'] for b in batches) == 1
    assert sql(f"SELECT COUNT(*) FROM biz_external_ticket WHERE prize_id={pid2} AND status='reserved'") == '1'
    win2 = next(b['data']['codes'][0]['code'] for b in batches if b['data']['winCount'])
    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(lambda value: draw(win2, value), [token, other]))
    assert sum(r['code'] == 0 for r in results) == 1
    assert sql(f'SELECT COUNT(DISTINCT reward_id) FROM biz_external_ticket WHERE prize_id={pid2}') == '1'
    passed('Concurrent batches and concurrent member draws cannot allocate or issue the same ticket twice')
    (out / 'api-result.json').write_text(json.dumps({'checks': checks, 'passed': len(checks),
        'scope': 'local database and HTTP only; no deployment; no actual admission tickets'}, ensure_ascii=False, indent=2), encoding='utf-8')
finally:
    if members:
        mids = ','.join(map(str, members))
        sql(f'DELETE FROM biz_external_ticket WHERE reward_id IN (SELECT reward_id FROM biz_reward WHERE member_id IN ({mids})); DELETE FROM biz_reward WHERE member_id IN ({mids}); DELETE FROM biz_member_session WHERE member_id IN ({mids}); DELETE FROM biz_mini_idempotency WHERE member_id IN ({mids}); DELETE FROM biz_point_account WHERE member_id IN ({mids}); DELETE FROM biz_member WHERE member_id IN ({mids});')
    for aid in activities:
        sql(f'DELETE FROM biz_external_ticket WHERE prize_id IN (SELECT prize_id FROM biz_lottery_prize WHERE activity_id={aid}); DELETE FROM biz_lottery_code_outcome WHERE code_id IN (SELECT code_id FROM biz_lottery_code WHERE activity_id={aid}); DELETE FROM biz_lottery_record WHERE activity_id={aid}; DELETE FROM biz_lottery_code WHERE activity_id={aid}; DELETE FROM biz_lottery_batch WHERE activity_id={aid}; DELETE FROM biz_lottery_prize WHERE activity_id={aid}; DELETE FROM biz_lottery_activity WHERE activity_id={aid};')
