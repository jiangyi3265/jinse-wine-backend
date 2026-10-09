"""Local API regression with disposable fixture rows; never calls payment APIs.

Load secrets/local-development.ps1 first. Uses only 127.0.0.1 and jinse_wine.
"""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import urllib.error
import urllib.request
import uuid

root = Path(__file__).resolve().parent.parent
mysql = shutil.which('mysql.exe')
assert mysql and os.environ.get('DB_PASSWORD') and os.environ.get('DB_USERNAME')
env = dict(os.environ, MYSQL_PWD=os.environ['DB_PASSWORD'])
prefix = 'refund-benefits-' + uuid.uuid4().hex
members, orders = [], []
store = None
checks = []

def sql(query):
    result = subprocess.run([mysql, '--protocol=TCP', '--host=127.0.0.1', '--port=3306',
                             '--user=' + os.environ['DB_USERNAME'], '--default-character-set=utf8mb4',
                             '--batch', '--skip-column-names', 'jinse_wine'],
                            input=query.encode(), stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                            env=env, timeout=30)
    if result.returncode:
        raise RuntimeError('Local fixture SQL failed: ' + result.stderr.decode(errors='replace')[:180])
    return result.stdout.decode().strip()

def request(path, body, token='', key='', method='POST'):
    headers = {'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token}
    if key: headers['Idempotency-Key'] = key
    req = urllib.request.Request('http://127.0.0.1:8080' + path,
                                 data=json.dumps(body).encode(), headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as response: return json.load(response)
    except urllib.error.HTTPError as error:
        return json.load(error)

def member(label, store_id=None):
    identity = prefix + '-' + label
    mid = int(sql("INSERT INTO biz_member(wechat_openid,nickname,staff_store_id) VALUES('" + identity + "','Regression fixture'," + (str(store_id) if store_id else 'NULL') + "); SELECT LAST_INSERT_ID();"))
    members.append(mid)
    token = uuid.uuid4().hex + uuid.uuid4().hex
    digest = hashlib.sha256(token.encode()).hexdigest()
    sql(f"INSERT INTO biz_member_session(token_hash,member_id,expires_at) VALUES('{digest}',{mid},DATE_ADD(NOW(),INTERVAL 10 MINUTE));")
    return mid, token

def assert_ledger(order_id, buyer, expected_points, expected_commission):
    assert int(sql(f"SELECT balance FROM biz_point_account WHERE member_id={buyer};")) == expected_points
    assert int(sql(f"SELECT SUM(change_amount) FROM biz_point_record WHERE member_id={buyer};")) == expected_points
    assert int(sql(f"SELECT SUM(amount_cent) FROM biz_promotion_record WHERE order_id={order_id};")) == expected_commission
    assert int(sql(f"SELECT COUNT(*) FROM biz_promotion_record WHERE order_id={order_id};")) == 1
    assert int(sql(f"SELECT COUNT(*) FROM biz_point_record WHERE member_id={buyer} AND source_type='order_refund';")) == 1

try:
    rule_raw = sql("SELECT rule_json,daily_limit FROM biz_point_rule WHERE rule_code='order_paid' AND status='0';")
    rule, daily = rule_raw.split('\t')
    multiplier = json.loads(rule).get('per_cent', 1)
    assert multiplier > 0 and int(daily) != 0, 'Enable order earning for this local verification'
    config = sql("SELECT customer_rate_bp,level_rates_json,refund_policy,min_withdraw_cent FROM biz_promotion_config WHERE status='0' ORDER BY config_id LIMIT 1;").split('\t')
    rate, schedule, policy, minimum = int(config[0]), json.loads(config[1]), config[2], int(config[3])
    if isinstance(schedule, dict) and schedule.get('customer'): rate = schedule['customer'][0]
    assert policy == 'reverse' and rate > 0
    original_commission = 10 * rate
    expected_commission = original_commission - original_commission * 20000 // 100000
    assert expected_commission >= max(1, minimum)
    assert int(sql("SELECT COUNT(*) FROM biz_product WHERE product_id=1 AND status='0';")) == 1
    login = request('/login', {'username': 'admin', 'password': 'admin123', 'code': ''})
    assert login['code'] == 200
    admin = login['token']
    store = int(sql(f"INSERT INTO biz_store(store_code,store_name) VALUES('{prefix}','Regression store'); SELECT LAST_INSERT_ID();"))
    promoter, promoter_token = member('promoter')
    staff, staff_token = member('staff', store)
    for mode in ('delivery', 'pickup', 'admin'):
        buyer, token = member(mode)
        order_no = prefix + '-' + mode
        key = uuid.uuid4().hex
        fulfillment = 'pickup' if mode == 'pickup' else 'delivery'
        initial = 'paid' if mode == 'pickup' else 'receiving'
        sql(f"INSERT INTO biz_promotion_relation(member_id,inviter_member_id,invite_code,status) VALUES({buyer},{promoter},'{uuid.uuid4().hex}','0');")
        oid = int(sql(f"INSERT INTO biz_order(order_no,member_id,address_snapshot,subtotal_cent,total_cent,request_id,status,paid_at,fulfillment_mode,pickup_store_id) VALUES('{order_no}',{buyer},'{{}}',100000,100000,'{key}','{initial}',NOW(),'{fulfillment}',{store if mode == 'pickup' else 'NULL'}); SELECT LAST_INSERT_ID();"))
        orders.append(oid)
        sql(f"INSERT INTO biz_order_item(order_id,product_id,product_name,unit_price_cent,quantity,total_cent) VALUES({oid},1,'Regression fixture',100000,1,100000);")
        refund = request('/mini/orders/' + order_no + '/refund', {'amount': 20000, 'reason': 'Isolated regression'}, token, uuid.uuid4().hex)
        assert refund['code'] == 0, mode + ' refund request'
        def complete():
            if mode == 'pickup': return request('/mini/staff/orders/confirm', {'orderNo': order_no}, staff_token, key)
            if mode == 'admin': return request(f'/business/order/{oid}/status', {'status': 'complete'}, admin, method='PUT')
            return request('/mini/orders/' + order_no + '/receive', {'requestId': key}, token)
        assert complete()['code'] == 409, mode + ' must block pending refunds'
        assert sql(f"SELECT status FROM biz_order WHERE order_id={oid};") == initial
        # Fixture-only confirmation; no external or real manual payment is represented.
        sql(f"UPDATE biz_order_refund SET status='refunded',channel_status='FIXTURE' WHERE order_id={oid};")
        assert complete()['code'] == (200 if mode == 'admin' else 0), mode + ' completion'
        assert_ledger(oid, buyer, 800 * multiplier, expected_commission)
        complete()
        assert_ledger(oid, buyer, 800 * multiplier, expected_commission)
        checks.append(mode + ': pending refund blocked, prior partial refund deducted, repeat does not duplicate rewards')
        if mode == 'delivery':
            sql(f"UPDATE biz_promotion_record SET status='settled' WHERE order_id={oid};")
            pending_key = uuid.uuid4().hex
            body = {'amount': 10000, 'reason': 'Isolated freeze regression'}
            first = request('/mini/orders/' + order_no + '/refund', body, token, pending_key)
            again = request('/mini/orders/' + order_no + '/refund', body, token, pending_key)
            assert first['code'] == 0 and again['data']['id'] == first['data']['id']
            assert sql(f"SELECT CONCAT(status,'|',refund_hold_status) FROM biz_promotion_record WHERE order_id={oid};") == 'closed|settled'
            withdrawal = request('/mini/promotion/withdraw', {'amount': expected_commission}, promoter_token, uuid.uuid4().hex)
            assert withdrawal['code'] == 409
            assert int(sql(f"SELECT COUNT(*) FROM biz_promotion_settlement WHERE member_id={promoter};")) == 0
            rejected = request('/business/extension/refunds/' + str(first['data']['id']) + '/status', {'status': 'rejected'}, admin, method='PUT')
            assert rejected['code'] == 200
            assert sql(f"SELECT CONCAT(status,'|',COALESCE(refund_hold_status,'none')) FROM biz_promotion_record WHERE order_id={oid};") == 'settled|none'
            checks.append('new refund holds commission immediately, retry is idempotent, withdrawal rejected, rejection releases hold')
finally:
    if members:
        ids = ','.join(map(str, members))
        sql(f"DELETE FROM biz_promotion_settlement_item WHERE settlement_id IN (SELECT settlement_id FROM biz_promotion_settlement WHERE member_id IN ({ids}));")
        sql(f"DELETE FROM biz_promotion_settlement WHERE member_id IN ({ids});")
    if orders:
        ids = ','.join(map(str, orders))
        for table in ('biz_order_pickup_redemption', 'biz_order_refund', 'biz_promotion_record', 'biz_order_item'):
            sql(f"DELETE FROM {table} WHERE order_id IN ({ids});")
        sql(f"DELETE FROM biz_order WHERE order_id IN ({ids}) AND order_no LIKE '{prefix}%';")
    if members:
        ids = ','.join(map(str, members))
        for table in ('biz_point_record', 'biz_point_account', 'biz_member_session', 'biz_promotion_relation'):
            sql(f"DELETE FROM {table} WHERE member_id IN ({ids});")
        sql(f"DELETE FROM biz_member WHERE member_id IN ({ids}) AND wechat_openid LIKE '{prefix}%';")
        assert int(sql(f"SELECT COUNT(*) FROM biz_member WHERE wechat_openid LIKE '{prefix}%';")) == 0
    if store: sql(f"DELETE FROM biz_store WHERE store_id={store} AND store_code='{prefix}';")
    assert int(sql(f"SELECT COUNT(*) FROM biz_order WHERE order_no LIKE '{prefix}%';")) == 0

report = {'localApi': 'http://127.0.0.1:8080', 'checks': checks, 'fixturesRemoved': True,
          'expectedPointsPerOrder': 800 * multiplier, 'expectedCommissionCentPerOrder': expected_commission,
          'externalPaymentCalls': 0, 'adminAuditRecordsRetained': True}
(root / 'docs/evidence/20261007-refund-benefits-api.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, ensure_ascii=False))
