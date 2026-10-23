"""Read-only existing payment/refund and official bill audit on the production Linux host."""
import base64
import csv
import datetime
from decimal import Decimal
import hashlib
import io
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import time
from urllib.error import HTTPError
from urllib.parse import quote, urlsplit
from urllib.request import Request, urlopen
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding

pid = subprocess.check_output(['systemctl', 'show', 'marathon.service', '-p', 'MainPID', '--value'], text=True).strip()
env = dict(item.split('=', 1) for item in Path('/proc/' + pid + '/environ').read_bytes().decode().split('\0') if '=' in item)
db_url = env['DB_URL']
database = urlsplit(db_url[5:] if db_url.startswith('jdbc:') else db_url)
if database.path != '/jinse_wine' or database.hostname not in ('127.0.0.1', 'localhost'):
    raise RuntimeError('Unexpected database; no audit performed')
mysql = [shutil.which('mysql') or '/www/server/mysql/bin/mysql', '--host=' + database.hostname,
         '--port=' + str(database.port or 3306), '--user=' + env['DB_USERNAME'],
         '--database=jinse_wine', '--batch', '--skip-column-names', '--raw', '--default-character-set=utf8mb4']

def rows(statement):
    response = subprocess.run(mysql + ['--execute=' + statement], env=dict(os.environ, MYSQL_PWD=env['DB_PASSWORD']),
                              capture_output=True, text=True, timeout=15)
    if response.returncode:
        raise RuntimeError('Read-only query failed; private diagnostics withheld')
    return [json.loads(line) for line in response.stdout.splitlines()]

private_key = serialization.load_pem_private_key(Path(env['MINI_WECHAT_PRIVATE_KEY']).read_bytes(), password=None)
platform_pem = Path(env['MINI_WECHAT_PLATFORM_CERT']).read_bytes()
if b'-----BEGIN CERTIFICATE-----' in platform_pem:
    certificate = x509.load_pem_x509_certificate(platform_pem)
    platform_key = certificate.public_key()
    platform_serial = env.get('MINI_WECHAT_PLATFORM_SERIAL') or format(certificate.serial_number, 'x')
else:
    platform_key = serialization.load_pem_public_key(platform_pem)
    platform_serial = env['MINI_WECHAT_PLATFORM_SERIAL']

def get(path, verify_signature=True):
    timestamp, nonce = str(int(time.time())), secrets.token_hex(16)
    signature = base64.b64encode(private_key.sign(('GET\n' + path + '\n' + timestamp + '\n' + nonce + '\n\n').encode(),
                                                 padding.PKCS1v15(), hashes.SHA256())).decode()
    authorization = ('WECHATPAY2-SHA256-RSA2048 mchid="' + env['MINI_WECHAT_MERCHANT_ID'] + '",'
                     'nonce_str="' + nonce + '",timestamp="' + timestamp + '",'
                     'serial_no="' + env['MINI_WECHAT_MERCHANT_SERIAL'] + '",signature="' + signature + '"')
    request = Request('https://api.mch.weixin.qq.com' + path, headers={'Authorization': authorization,
                      'Accept': 'application/json' if verify_signature else '*/*', 'User-Agent': 'Jinse-read-only-audit/1.0'})
    try:
        response = urlopen(request, timeout=12)
    except HTTPError as error:
        response = error
    with response:
        body, status, headers = response.read(4 * 1024 * 1024 + 1), response.status, response.headers
    if len(body) > 4 * 1024 * 1024:
        raise RuntimeError('Official audit response exceeds the bounded size')
    if verify_signature:
        stamp, response_nonce = headers.get('Wechatpay-Timestamp'), headers.get('Wechatpay-Nonce')
        if not stamp or not response_nonce or not headers.get('Wechatpay-Signature') or headers.get('Wechatpay-Serial', '').lower() != platform_serial.lower():
            raise RuntimeError('Official response signature headers do not match configured verification material')
        if abs(time.time() - int(stamp)) > 300:
            raise RuntimeError('Official response timestamp is outside the verification window')
        platform_key.verify(base64.b64decode(headers['Wechatpay-Signature'], validate=True),
                            stamp.encode() + b'\n' + response_nonce.encode() + b'\n' + body + b'\n',
                            padding.PKCS1v15(), hashes.SHA256())
        return status, json.loads(body)
    return status, body

orders = rows("SELECT JSON_OBJECT('orderNo',order_no,'totalCent',total_cent,'shippingCent',shipping_cent,'subtotalCent',subtotal_cent,'localStatus',status,'transactionId',wechat_transaction_id,'paidDate',DATE_FORMAT(paid_at,'%Y-%m-%d')) FROM biz_order WHERE wechat_transaction_id IS NOT NULL AND wechat_transaction_id<>'' ORDER BY order_id LIMIT 101")
refunds = rows("SELECT JSON_OBJECT('refundNo',r.refund_no,'orderNo',o.order_no,'totalCent',o.total_cent,'amountCent',r.amount_cent,'localStatus',r.status,'channelStatus',r.channel_status,'refundId',r.channel_refund_id,'transactionId',o.wechat_transaction_id) FROM biz_order_refund r JOIN biz_order o ON o.order_id=r.order_id WHERE r.channel_refund_id IS NOT NULL AND r.channel_refund_id<>'' ORDER BY r.refund_id LIMIT 101")
if len(orders) > 100 or len(refunds) > 100:
    raise RuntimeError('Audit exceeds bounded live-query scope; no partial success receipt')
result = {'observedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'pid': int(pid),
          'scope': 'read-only official queries of already existing payment/refund records; no new money movement or business updates',
          'paymentQueries': [], 'refundQueries': [], 'billQueries': [], 'tradeBills': []}
for order in orders:
    status, trade = get('/v3/pay/transactions/out-trade-no/' + quote(order['orderNo'], safe='') + '?mchid=' + quote(env['MINI_WECHAT_MERCHANT_ID'], safe=''))
    if status != 200:
        raise RuntimeError('Existing payment could not be verified against official trade query')
    matched = (trade.get('out_trade_no') == order['orderNo'] and trade.get('transaction_id') == order['transactionId']
               and trade.get('appid') == env['MINI_WECHAT_APP_ID'] and trade.get('mchid') == env['MINI_WECHAT_MERCHANT_ID']
               and trade.get('amount', {}).get('total') == order['totalCent'] and trade.get('amount', {}).get('currency') == 'CNY')
    if not matched or trade.get('trade_state') not in ('SUCCESS', 'REFUND'):
        raise RuntimeError('Existing payment identity, amount or paid state differs from official trade')
    result['paymentQueries'].append({'orderReferenceSha256': hashlib.sha256(order['orderNo'].encode()).hexdigest(),
        'localStatus': order['localStatus'], 'platformState': trade['trade_state'], 'totalCent': order['totalCent'],
        'shippingCent': order['shippingCent'], 'subtotalCent': order['subtotalCent'], 'paidDate': order['paidDate'],
        'responseSignatureVerified': True, 'identityAndAmountMatch': True})
for refund in refunds:
    status, data = get('/v3/refund/domestic/refunds/' + quote(refund['refundNo'], safe=''))
    if status != 200:
        raise RuntimeError('Existing refund could not be verified against official refund query')
    matched = (data.get('out_refund_no') == refund['refundNo'] and data.get('out_trade_no') == refund['orderNo']
               and data.get('refund_id') == refund['refundId'] and data.get('transaction_id') == refund['transactionId']
               and data.get('amount', {}).get('total') == refund['totalCent']
               and data.get('amount', {}).get('refund') == refund['amountCent'] and data.get('amount', {}).get('currency') == 'CNY')
    if not matched or data.get('status') != refund['channelStatus']:
        raise RuntimeError('Existing refund identity, amount or state differs from official refund')
    refund['successDate'] = str(data.get('success_time', ''))[:10]
    result['refundQueries'].append({'refundReferenceSha256': hashlib.sha256(refund['refundNo'].encode()).hexdigest(),
        'orderReferenceSha256': hashlib.sha256(refund['orderNo'].encode()).hexdigest(), 'localStatus': refund['localStatus'],
        'platformState': data['status'], 'amountCent': refund['amountCent'], 'totalCent': refund['totalCent'],
        'responseSignatureVerified': True, 'identityAndAmountMatch': True, 'successTimePresent': bool(data.get('success_time')),
        'successDate': refund['successDate']})
bill_dates = {order['paidDate'] for order in orders if order['paidDate']}
bill_dates.update(refund['successDate'] for refund in refunds if refund['channelStatus'] == 'SUCCESS' and refund['successDate'])
for date in sorted(bill_dates):
    datetime.date.fromisoformat(date)
    for submitted_date in (date, date.replace('-', '')):
        status, data = get('/v3/bill/tradebill?bill_date=' + submitted_date + '&bill_type=ALL')
        result['billQueries'].append({'billDate': date, 'submittedBillDate': submitted_date,
                                     'httpStatus': status, 'platformCode': data.get('code'),
                                     'platformMessage': data.get('message'),
                                     'responseSignatureVerified': True, 'downloadAvailable': bool(data.get('download_url'))})
        if status == 200 and data.get('download_url'):
            download = urlsplit(data['download_url'])
            if download.scheme != 'https' or download.hostname != 'api.mch.weixin.qq.com' or download.username or download.password or download.port or not download.path.startswith('/v3/billdownload/'):
                raise RuntimeError('Unexpected official bill download URL')
            download_status, bill = get(download.path + '?' + download.query, verify_signature=False)
            algorithm = str(data.get('hash_type', '')).upper()
            if download_status != 200 or algorithm not in ('SHA1', 'SHA256'):
                raise RuntimeError('Official bill download or digest algorithm is invalid')
            digest = hashlib.new(algorithm.lower(), bill).hexdigest()
            if digest.lower() != str(data.get('hash_value', '')).lower():
                raise RuntimeError('Downloaded bill differs from the digest in the signed response')
            private_bill = Path('/www/backup/jinse-wine/wechat-trade-bill-' + date + '.csv')
            private_bill.write_bytes(bill)
            private_bill.chmod(0o600)
            csv_rows = list(csv.reader(io.StringIO(bill.decode('utf-8-sig'))))
            normalize = lambda value: value.strip().lstrip('`').strip()
            headers = [normalize(value) for value in csv_rows[0]] if csv_rows else []
            safe_rows = []
            summary_rows = []
            paid = {order['orderNo']: order for order in orders if order['paidDate'] == date}
            returned = {refund['refundNo']: refund for refund in refunds if refund['channelStatus'] == 'SUCCESS' and refund['successDate'] == date}
            seen_payments, seen_refunds = set(), set()
            payment_cent, refund_cent = 0, 0
            for values in csv_rows[1:]:
                values = [normalize(value) for value in values]
                if len(values) == len(headers):
                    row = dict(zip(headers, values))
                    safe = {'tradeState': row.get('交易状态'), 'refundState': row.get('退款状态'),
                            'orderReferenceSha256': hashlib.sha256(row.get('商户订单号', '').encode()).hexdigest(),
                            'refundReferenceSha256': hashlib.sha256(row.get('商户退款单号', '').encode()).hexdigest() if row.get('商户退款单号') not in ('', '0') else None,
                            'currency': row.get('货币种类')}
                    for column in ('应结订单金额', '代金券金额', '退款金额', '手续费', '订单金额', '申请退款金额'):
                        value = Decimal(row[column]) * 100
                        if value != value.to_integral_value():
                            raise RuntimeError('Official bill has fractional cents')
                        safe[column] = int(value)
                    if row.get('公众账号ID') != env['MINI_WECHAT_APP_ID'] or row.get('商户号') != env['MINI_WECHAT_MERCHANT_ID'] or row.get('货币种类') != 'CNY':
                        raise RuntimeError('Bill AppID, merchant or currency does not match this application')
                    if row.get('交易状态') == 'SUCCESS':
                        order = paid.get(row.get('商户订单号'))
                        if not order or order['orderNo'] in seen_payments or row.get('微信订单号') != order['transactionId'] or safe['订单金额'] != order['totalCent'] or safe['退款金额'] != 0:
                            raise RuntimeError('Bill payment identity, amount or uniqueness differs from local and official query evidence')
                        seen_payments.add(order['orderNo'])
                        payment_cent += safe['订单金额']
                    elif row.get('交易状态') == 'REFUND' and row.get('退款状态') == 'SUCCESS':
                        refund = returned.get(row.get('商户退款单号'))
                        if not refund or refund['refundNo'] in seen_refunds or row.get('商户订单号') != refund['orderNo'] or row.get('微信订单号') != refund['transactionId'] or row.get('微信退款单号') != refund['refundId'] or safe['退款金额'] != refund['amountCent']:
                            raise RuntimeError('Bill refund identity, amount or uniqueness differs from local and official query evidence')
                        seen_refunds.add(refund['refundNo'])
                        refund_cent += safe['退款金额']
                    else:
                        raise RuntimeError('Unexpected trade state; no partial bill reconciliation accepted')
                    safe['identityAndAmountMatch'] = True
                    safe_rows.append(safe)
                else:
                    summary_rows.append(values)
            if seen_payments != set(paid) or seen_refunds != set(returned):
                raise RuntimeError('Bill is missing a recorded successful payment or refund')
            if len(summary_rows) != 2 or len(summary_rows[0]) != len(summary_rows[1]):
                raise RuntimeError('Unexpected bill summary structure')
            summary = dict(zip(summary_rows[0], summary_rows[1]))
            if int(summary['总交易单数']) != len(safe_rows) or Decimal(summary['订单总金额']) * 100 != payment_cent or Decimal(summary['退款总金额']) * 100 != refund_cent or Decimal(summary['申请退款总金额']) * 100 != refund_cent:
                raise RuntimeError('Bill summary does not match reconciled payment/refund rows')
            result['tradeBills'].append({'billDate': date, 'querySignatureVerified': True,
                'hashType': algorithm, 'officialFileHash': digest, 'sha256': hashlib.sha256(bill).hexdigest(),
                'bytes': len(bill), 'headers': headers, 'rows': safe_rows, 'summaryRows': summary_rows,
                'csvRowsAfterHeader': len(csv_rows) - 1, 'rowFieldCounts': sorted({len(row) for row in csv_rows[1:]}),
                'matchedPaymentCount': len(seen_payments), 'matchedRefundCount': len(seen_refunds),
                'paymentCent': payment_cent, 'refundCent': refund_cent, 'netSalesCent': payment_cent - refund_cent,
                'summaryMatched': True, 'fileStoredPrivately': True, 'reconciled': True})
result['allExistingRecordedWechatPaymentsAndRefundsQueried'] = True
target = Path('/www/backup/jinse-wine/wechat-existing-payments-audit.json')
target.write_text(json.dumps(result, indent=2) + '\n')
target.chmod(0o600)
print(json.dumps(result))
