"""Local fixed-outcome API regression. Uses disposable rows in local jinse_wine only.

Load secrets/local-development.ps1 first. No WeChat, payment or notification API
is called. Login/operation audit records are retained; fixture rows are removed.
"""
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import urllib.error
import urllib.request
import uuid

root = Path(__file__).resolve().parent.parent
mysql = shutil.which('mysql.exe') or 'D:/zhangq/mysql/bin/mysql.exe'
assert os.environ.get('DB_PASSWORD') and os.environ.get('DB_USERNAME')
env = dict(os.environ, MYSQL_PWD=os.environ['DB_PASSWORD'])
prefix = 'allocation-check-' + uuid.uuid4().hex[:16]
activities, members, templates, checks = [], [], [], []
admin = ''


def sql(query):
    result = subprocess.run([mysql, '--protocol=TCP', '--host=127.0.0.1', '--port=3306',
                             '--user=' + os.environ['DB_USERNAME'], '--default-character-set=utf8mb4',
                             '--batch', '--skip-column-names', 'jinse_wine'], input=query.encode(),
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=env, timeout=25)
    if result.returncode:
        raise RuntimeError('Local fixture SQL failed: ' + result.stderr.decode(errors='replace')[:200])
    return result.stdout.decode().strip()


def request(path, body=None, token='', key='', method=None):
    headers = {'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token}
    if key:
        headers['Idempotency-Key'] = key
    req = urllib.request.Request('http://127.0.0.1:8080' + path,
                                 data=None if body is None else json.dumps(body).encode(),
                                 headers=headers, method=method or ('GET' if body is None else 'POST'))
    try:
        with urllib.request.urlopen(req, timeout=25) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        return json.load(error)


def member():
    mid = int(sql(f"INSERT INTO biz_member(wechat_openid,nickname,status) VALUES('{prefix}-{len(members)}','Allocation fixture','0'); SELECT LAST_INSERT_ID();"))
    members.append(mid)
    token = uuid.uuid4().hex + uuid.uuid4().hex
    digest = hashlib.sha256(token.encode()).hexdigest()
    sql(f"INSERT INTO biz_member_session(token_hash,member_id,expires_at) VALUES('{digest}',{mid},DATE_ADD(NOW(),INTERVAL 15 MINUTE));")
    return mid, token


def activity(kind='points', stock=3, limit=100, coupon=None):
    code = prefix + '-' + str(len(activities))
    aid = int(sql(f"INSERT INTO biz_lottery_activity(activity_code,activity_name,daily_limit,total_limit,rule_json,end_time) VALUES('{code}','Allocation fixture',100,{limit},'{{\"codeReusable\":true,\"rewardValidityDays\":7}}',DATE_ADD(NOW(),INTERVAL 2 DAY)); SELECT LAST_INSERT_ID();"))
    activities.append(aid)
    pid = int(sql(f"INSERT INTO biz_lottery_prize(activity_id,prize_type,prize_name,probability_bp,points,stock,amount_min_cent,amount_max_cent,coupon_template_id) VALUES({aid},'{kind}','Original fixture',10000,37,{stock},123,123,{coupon or 'NULL'}); SELECT LAST_INSERT_ID();"))
    return aid, pid, code


def generate(code, count, batch=None):
    return request('/business/extension/lottery/codes',
                   {'activityCode': code, 'count': count, 'requestId': batch or uuid.uuid4().hex}, admin)


def draw(code, token, key=None):
    return request('/mini/draws', {'code': code}, token, key or uuid.uuid4().hex)


def passed(label):
    checks.append(label)
    print('PASS: ' + label, flush=True)


try:
    login = request('/login', {'username': 'admin', 'password': 'admin123', 'code': ''})
    assert login['code'] == 200, 'Local admin login failed'
    admin = login['token']
    mid, token = member()
    _, other = member()
    aid, pid, code = activity()
    key = uuid.uuid4().hex
    batch = generate(code, 5, key)
    assert batch['code'] == 200, batch
    codes = batch['data']['codes']
    assert len(codes) == 5 and batch['data']['winCount'] == 3
    assert sql(f'SELECT stock FROM biz_lottery_prize WHERE prize_id={pid}') == '0'
    assert len({c['code'] for c in codes}) == 5 and all(len(c['code']) <= 32 for c in codes)
    assert all(c['linkUrl'].endswith(c['code']) for c in codes)
    repeated = generate(code, 5, key)
    assert repeated['data'] == batch['data']
    assert generate(code, 4, key)['code'] != 200
    loaded = request('/business/extension/lottery/code-batches/' + key, token=admin)
    assert loaded['data'] == batch['data']
    assert request('/business/extension/lottery/code-batches/' + key, token=token)['code'] != 200
    passed('generation reserves finite stock; shortage becomes loss; retry/history preserve batch; member cannot read outcomes')

    sql(f"UPDATE biz_lottery_prize SET points=999,prize_name='Changed fixture',probability_bp=0,status='1' WHERE prize_id={pid}")
    win = next(c for c in codes if c['resultType'] == 'points')['code']
    loss = next(c for c in codes if c['resultType'] == 'none')['code']
    request_id = uuid.uuid4().hex
    first = draw(win, token, request_id)
    assert first['code'] == 0 and first['data']['kind'] == 'points' and first['data']['value'] == '37积分', first
    replay = draw(win, token, request_id)
    assert replay['code'] == 0 and replay['data'] == first['data'], replay
    assert draw(win, other)['code'] == 409
    assert draw(win, token)['code'] == 409
    assert draw(loss, token)['data']['kind'] == 'none'
    assert sql(f'SELECT balance FROM biz_point_account WHERE member_id={mid}') == '37'
    assert sql(f'SELECT stock FROM biz_lottery_prize WHERE prize_id={pid}') == '0'
    passed('saved points survive prize edits/disable; single claim overrides legacy reusable flag; replay does not award twice; losing code stays losing')

    next_win = [c['code'] for c in codes if c['resultType'] == 'points'][1]
    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(lambda t: draw(next_win, t), [token, other]))
    assert sum(r['code'] == 0 for r in results) == 1, results
    assert sql(f'SELECT COUNT(*) FROM biz_lottery_record WHERE activity_id={aid}') == '3'
    passed('two members claiming the same code concurrently produce exactly one award')

    cap_id, _, cap_code = activity(stock=10, limit=2)
    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(lambda _: generate(cap_code, 2), range(2)))
    assert sum(r['code'] == 200 for r in results) == 1, results
    assert sql(f'SELECT COUNT(*) FROM biz_lottery_code WHERE activity_id={cap_id}') == '2'
    legacy = 'LEG-' + uuid.uuid4().hex
    sql(f"INSERT INTO biz_lottery_code(activity_id,draw_code,status) VALUES({cap_id},'{legacy}','unused')")
    assert draw(legacy, token)['code'] == 409
    edit_body = {'activityCode': cap_code, 'activityName': 'Allocation fixture',
                 'dailyLimit': 100, 'totalLimit': 1, 'status': '0', 'ruleJson': '{}'}
    edit = request('/business/extension/lottery/activity', edit_body, admin, method='PUT')
    assert edit['code'] != 200
    assert sql(f'SELECT total_limit FROM biz_lottery_activity WHERE activity_id={cap_id}') == '2'
    edit_body['totalLimit'] = 3
    assert request('/business/extension/lottery/activity', edit_body, admin, method='PUT')['code'] == 200
    passed('concurrent batches cannot exceed activity capacity; legacy draw and limit reduction respect reserved slots')

    for kind in ('goods', 'cash'):
        reward_aid, reward_pid, reward_code = activity(kind, stock=1)
        fixed = generate(reward_code, 1)['data']['codes'][0]
        sql(f"UPDATE biz_lottery_prize SET prize_name='Changed reward',amount_min_cent=999,amount_max_cent=999,status='1' WHERE prize_id={reward_pid}")
        result = draw(fixed['code'], token)
        assert result['code'] == 0 and result['data']['kind'] == kind, result
        reward = sql(f"SELECT CONCAT(reward_name,'|',amount_cent,'|',IF(expires_at IS NULL,0,1)) FROM biz_reward WHERE member_id={mid} AND reward_type='{kind}'")
        assert reward == 'Original fixture|' + ('123' if kind == 'cash' else '0') + '|1', reward
        assert sql(f'SELECT stock FROM biz_lottery_prize WHERE prize_id={reward_pid}') == '0'
    passed('goods and cash reward records use fixed names/amount and saved validity; no transfer API invoked')

    coupon = int(sql(f"INSERT INTO biz_coupon_template(template_code,template_name,coupon_type,amount_cent,stock,end_time) VALUES('{prefix}','Allocation coupon','fixed',200,2,DATE_ADD(NOW(),INTERVAL 1 DAY)); SELECT LAST_INSERT_ID();"))
    templates.append(coupon)
    coupon_aid, coupon_pid, coupon_code = activity('coupon', stock=2, coupon=coupon)
    generated = generate(coupon_code, 2)
    assert generated['code'] == 200, generated
    coupon_batch = generated['data']['codes']
    assert sql(f'SELECT stock FROM biz_coupon_template WHERE coupon_template_id={coupon}') == '0'
    sql(f"UPDATE biz_coupon_template SET amount_cent=999,status='1' WHERE coupon_template_id={coupon}")
    claimed = draw(coupon_batch[0]['code'], token)
    assert claimed['code'] == 0 and claimed['data']['kind'] == 'coupon', claimed
    assert sql(f"SELECT JSON_UNQUOTE(JSON_EXTRACT(rule_snapshot,'$.amount_cent')) FROM biz_member_coupon WHERE member_id={mid} AND coupon_template_id={coupon}") == '200'
    sql(f"UPDATE biz_lottery_code_outcome o JOIN biz_lottery_code c ON c.code_id=o.code_id SET o.expires_at=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE c.activity_id={coupon_aid} AND o.status='reserved'")
    assert draw(coupon_batch[1]['code'], token)['code'] == 409
    deadline = time.monotonic() + 80
    while time.monotonic() < deadline:
        if sql(f"SELECT COUNT(*) FROM biz_lottery_code_outcome o JOIN biz_lottery_code c ON c.code_id=o.code_id WHERE c.activity_id={coupon_aid} AND o.status='expired'") == '1':
            break
        time.sleep(2)
    else:
        raise AssertionError('Scheduled reservation expiry did not finish within 80 seconds')
    assert sql(f'SELECT stock FROM biz_coupon_template WHERE coupon_template_id={coupon}') == '1'
    assert sql(f'SELECT stock FROM biz_lottery_prize WHERE prize_id={coupon_pid}') == '1'
    passed('coupon rule snapshot survives template edits; expired code refuses claim; scheduler returns both prize and coupon stock')

finally:
    if activities:
        ids = ','.join(map(str, activities))
        sql(f'DELETE FROM biz_lottery_record WHERE activity_id IN ({ids}); DELETE o FROM biz_lottery_code_outcome o JOIN biz_lottery_code c ON c.code_id=o.code_id WHERE c.activity_id IN ({ids}); DELETE FROM biz_lottery_code WHERE activity_id IN ({ids}); DELETE FROM biz_lottery_batch WHERE activity_id IN ({ids}); DELETE FROM biz_lottery_prize WHERE activity_id IN ({ids}); DELETE FROM biz_lottery_activity WHERE activity_id IN ({ids});')
    if members:
        ids = ','.join(map(str, members))
        for table in ('biz_member_coupon', 'biz_reward', 'biz_lottery_record', 'biz_mini_idempotency',
                      'biz_point_record', 'biz_point_account', 'biz_member_session', 'biz_member'):
            sql(f'DELETE FROM {table} WHERE member_id IN ({ids})')
    if templates:
        sql('DELETE FROM biz_coupon_template WHERE coupon_template_id IN (' + ','.join(map(str, templates)) + ')')

report = {'at': datetime.now(timezone.utc).isoformat(), 'environment': 'local 127.0.0.1:8080 / jinse_wine',
          'checks': checks, 'fixtureCleanup': 'completed', 'externalWeChatCalls': False,
          'limitations': 'Not a production, real-device, payment or print-scan acceptance result.'}
out = root / 'docs/evidence/20261007-lottery-preallocation-local.json'
out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
print('Evidence: ' + str(out))
