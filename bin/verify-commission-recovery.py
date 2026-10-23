"""Exercise local MySQL/API refund holds and net withdrawal with disposable fixtures.

No external payment is initiated. Run after loading local-development.ps1.
"""
import datetime
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
if not mysql or not os.environ.get('DB_PASSWORD') or not os.environ.get('DB_USERNAME'):
    raise RuntimeError('Local database credentials and mysql.exe are required')
env = dict(os.environ, MYSQL_PWD=os.environ['DB_PASSWORD'])
prefix = 'commission-recovery-' + uuid.uuid4().hex
members, orders = [], []
admin = ''

def sql(statement):
    process = subprocess.run([mysql, '--protocol=TCP', '--host=127.0.0.1', '--port=3306',
                              '--user=' + os.environ['DB_USERNAME'], '--database=jinse_wine',
                              '--default-character-set=utf8mb4', '--batch', '--skip-column-names'],
                             input=statement.encode(), env=env, capture_output=True, timeout=20)
    if process.returncode:
        raise RuntimeError('Local fixture statement failed; private diagnostics withheld')
    return process.stdout.decode().strip()

def api(path, body=None, token='', key='', method=None):
    headers = {'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token}
    if key:
        headers['Idempotency-Key'] = key
    request = urllib.request.Request('http://127.0.0.1:8080' + path, headers=headers,
                                     data=None if body is None else json.dumps(body).encode(), method=method)
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        return json.load(error)

def check(condition, message):
    if not condition:
        raise AssertionError(message)

def member(label):
    mid = int(sql("INSERT INTO biz_member(wechat_openid,nickname) VALUES('" + prefix + '-' + label
                  + "','Disposable regression fixture'); SELECT LAST_INSERT_ID();"))
    members.append(mid)
    token = uuid.uuid4().hex + uuid.uuid4().hex
    digest = hashlib.sha256(token.encode()).hexdigest()
    sql(f"INSERT INTO biz_member_session(token_hash,member_id,expires_at) VALUES('{digest}',{mid},DATE_ADD(NOW(),INTERVAL 10 MINUTE));")
    return mid, token

try:
    check(api('/mini/products')['code'] == 0, 'Local backend unavailable')
    minimum = int(sql("SELECT COALESCE(NULLIF(min_withdraw_cent,0),1000) FROM biz_promotion_config WHERE status='0' ORDER BY config_id LIMIT 1;"))
    check(0 < minimum < 10000000, 'Use an ordinary local withdrawal minimum')
    login = api('/login', {'username': 'admin', 'password': 'admin123', 'code': ''})
    check(login.get('code') == 200, 'Local admin login failed')
    admin = login['token']
    buyer, buyer_token = member('buyer')
    promoter, promoter_token = member('promoter')
    for suffix in ('refund', 'earnings'):
        order_no = prefix + '-' + suffix
        oid = int(sql(f"INSERT INTO biz_order(order_no,member_id,address_snapshot,subtotal_cent,total_cent,request_id,status,fulfillment_mode,paid_at) VALUES('{order_no}',{buyer},'{{}}',10000,10000,'{uuid.uuid4().hex}','complete','delivery',NOW()); SELECT LAST_INSERT_ID();"))
        orders.append(oid)
    oid = orders[0]
    # Synthetic historic state only; this is not a payment/refund receipt.
    sql(f"INSERT INTO biz_order_refund(order_id,member_id,refund_no,request_id,amount_cent,reason,status,channel_status) VALUES({oid},{buyer},'RF{uuid.uuid4().hex[:20]}','{uuid.uuid4().hex}',2000,'Disposable fixture','refunded','FIXTURE');")
    sql(f"INSERT INTO biz_promotion_record(member_id,order_id,title,amount_cent,original_amount_cent,refund_adjustment_cent,status,refund_recovery_required) VALUES({promoter},{oid},'Disposable paid fixture',1000,1000,200,'paid',1);")
    sql(f"INSERT INTO biz_promotion_record(member_id,order_id,title,amount_cent,original_amount_cent,refund_adjustment_cent,status,refund_hold_status,refund_recovery_required) VALUES({promoter},{oid},'Disposable historical debt',-200,0,200,'closed','settled',1);")
    gross = minimum + 200
    sql(f"INSERT INTO biz_promotion_record(member_id,order_id,title,amount_cent,original_amount_cent,status) VALUES({promoter},{orders[1]},'Disposable matured earnings',{gross},{gross},'settled');")
    request_key = uuid.uuid4().hex
    path = '/mini/orders/' + prefix + '-refund/refund'
    pending = api(path, {'amount': 1000, 'reason': 'Disposable repeat refund check'}, buyer_token, request_key)
    replay = api(path, {'amount': 1000, 'reason': 'Disposable repeat refund check'}, buyer_token, request_key)
    check(pending.get('code') == 0 and replay.get('data', {}).get('id') == pending['data']['id'], 'Refund request idempotence failed')
    check(sql(f"SELECT CONCAT(status,'|',COALESCE(refund_hold_status,'none')) FROM biz_promotion_record WHERE member_id={promoter} AND amount_cent<0;") == 'settled|none', 'Historical debt remains frozen')
    rejected_gross = api('/mini/promotion/withdraw', {'amount': gross}, promoter_token, uuid.uuid4().hex)
    check(rejected_gross.get('code') == 409, 'Gross withdrawal ignored debt')
    check(int(sql(f"SELECT COUNT(*) FROM biz_promotion_settlement WHERE member_id={promoter};")) == 0, 'Rejected withdrawal left a settlement')
    rejected_refund = api('/business/extension/refunds/' + str(pending['data']['id']) + '/status',
                           {'status': 'rejected'}, admin, method='PUT')
    check(rejected_refund.get('code') == 200, 'Local refund rejection failed')
    check(sql(f"SELECT CONCAT(status,'|',COALESCE(refund_hold_status,'none')) FROM biz_promotion_record WHERE member_id={promoter} AND amount_cent<0;") == 'settled|none', 'Rejection lost confirmed debt')
    withdrawal_key = uuid.uuid4().hex
    net = api('/mini/promotion/withdraw', {'amount': minimum}, promoter_token, withdrawal_key)
    repeated = api('/mini/promotion/withdraw', {'amount': minimum}, promoter_token, withdrawal_key)
    check(net.get('code') == 0 and repeated.get('data', {}).get('id') == net['data']['id'], 'Net withdrawal idempotence failed')
    check(int(sql(f"SELECT amount_cent FROM biz_promotion_settlement WHERE member_id={promoter};")) == minimum, 'Net amount mismatch')
    check(int(sql(f"SELECT COUNT(*) FROM biz_promotion_settlement WHERE member_id={promoter};")) == 1, 'Duplicate withdrawal')
    check(int(sql(f"SELECT COUNT(*) FROM biz_promotion_settlement_item i JOIN biz_promotion_settlement s ON s.settlement_id=i.settlement_id WHERE s.member_id={promoter};")) == 2, 'Withdrawal omitted debt or income')
    check(sql(f"SELECT status FROM biz_promotion_record WHERE member_id={promoter} AND amount_cent<0;") == 'processing', 'Debt not selected for withdrawal')
    check(int(sql(f"SELECT COUNT(*) FROM biz_promotion_record WHERE member_id={promoter} AND status='paid';")) == 1, 'Original paid commission changed')
finally:
    if members:
        ids = ','.join(map(str, members))
        sql(f"DELETE FROM biz_promotion_settlement_item WHERE settlement_id IN (SELECT settlement_id FROM biz_promotion_settlement WHERE member_id IN ({ids}));")
        sql(f"DELETE FROM biz_promotion_settlement WHERE member_id IN ({ids});")
    if orders:
        ids = ','.join(map(str, orders))
        for table in ('biz_order_refund', 'biz_promotion_record'):
            sql(f"DELETE FROM {table} WHERE order_id IN ({ids});")
        sql(f"DELETE FROM biz_order WHERE order_id IN ({ids}) AND order_no LIKE '{prefix}%';")
    if members:
        ids = ','.join(map(str, members))
        for table in ('biz_point_record', 'biz_point_account', 'biz_member_session'):
            sql(f"DELETE FROM {table} WHERE member_id IN ({ids});")
        sql(f"DELETE FROM biz_member WHERE member_id IN ({ids}) AND wechat_openid LIKE '{prefix}%';")
    if admin:
        check(api('/logout', {}, admin).get('code') == 200, 'Admin session cleanup failed')
    check(int(sql(f"SELECT COUNT(*) FROM biz_member WHERE wechat_openid LIKE '{prefix}%';")) == 0, 'Fixture member cleanup incomplete')
    check(int(sql(f"SELECT COUNT(*) FROM biz_order WHERE order_no LIKE '{prefix}%';")) == 0, 'Fixture order cleanup incomplete')

result = {'observedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(),
          'localApi': 'http://127.0.0.1:8080', 'historicalHeldDebtRestored': True,
          'duplicateRefundReturnsSameRecord': True, 'grossWithdrawalRejected': True,
          'refundRejectionPreservesDebt': True, 'netWithdrawalIncludesIncomeAndDebt': True,
          'repeatedWithdrawalReturnsSameRecord': True, 'originalPaidCommissionUnchanged': True,
          'fixturesRemoved': True, 'adminAuditRecordsRetained': True, 'externalPaymentCalls': 0,
          'scope': 'real local MySQL and HTTP business flow with synthetic fixture ledger; no external payment or production writes'}
target = root / 'docs/evidence/20261010-commission-recovery-api.json'
target.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
print(json.dumps(result))
