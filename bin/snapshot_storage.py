"""S3 transport shared by the joint snapshot export and isolated restore."""
import datetime
import hashlib
import hmac
from urllib.parse import quote, urlencode, urlsplit
from urllib.request import Request, urlopen
import xml.etree.ElementTree as ET


def sha(data):
    return hashlib.sha256(data).hexdigest()


class Storage:
    def __init__(self, endpoint, access, secret):
        self.endpoint = endpoint.rstrip('/')
        self.host = urlsplit(self.endpoint).netloc
        self.access, self.secret = access, secret
        if not access or not secret:
            raise ValueError('Object storage credentials are missing; values withheld')

    def request(self, method, bucket, key='', data=b'', query=None, metadata=None):
        stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
        date = stamp[:8]
        path = '/' + quote(bucket, safe='') + ('/' + quote(key, safe='/') if key else '')
        query_text = urlencode(sorted((query or {}).items()), quote_via=quote, safe='')
        headers = {'host': self.host, 'x-amz-date': stamp, 'x-amz-content-sha256': sha(data)}
        headers.update(metadata or {})
        names = ';'.join(sorted(headers))
        canonical = '\n'.join([method, path, query_text,
                               ''.join(k + ':' + str(headers[k]).strip() + '\n' for k in sorted(headers)), names, sha(data)])
        scope = date + '/us-east-1/s3/aws4_request'
        mac = lambda k, s: hmac.new(k, s.encode(), hashlib.sha256).digest()
        signing = mac(mac(mac(mac(('AWS4' + self.secret).encode(), date), 'us-east-1'), 's3'), 'aws4_request')
        signature = mac(signing, 'AWS4-HMAC-SHA256\n' + stamp + '\n' + scope + '\n' + sha(canonical.encode())).hex()
        headers['Authorization'] = 'AWS4-HMAC-SHA256 Credential=' + self.access + '/' + scope + ', SignedHeaders=' + names + ', Signature=' + signature
        address = self.endpoint + path + ('?' + query_text if query_text else '')
        with urlopen(Request(address, data=data if method in ('PUT', 'POST') else None, headers=headers, method=method), timeout=20) as response:
            return response.read(), {k.lower(): v for k, v in response.headers.items()}

    def objects(self, bucket):
        result, token = [], None
        ns = {'s': 'http://s3.amazonaws.com/doc/2006-03-01/'}
        while True:
            query = {'list-type': '2'}
            if token:
                query['continuation-token'] = token
            root = ET.fromstring(self.request('GET', bucket, query=query)[0])
            for item in root.findall('s:Contents', ns):
                result.append({'key': item.findtext('s:Key', namespaces=ns),
                               'bytes': int(item.findtext('s:Size', namespaces=ns)),
                               'etag': item.findtext('s:ETag', namespaces=ns),
                               'lastModified': item.findtext('s:LastModified', namespaces=ns)})
            if root.findtext('s:IsTruncated', namespaces=ns) != 'true':
                break
            token = root.findtext('s:NextContinuationToken', namespaces=ns)
            if not token:
                raise RuntimeError('Object listing continuation token is missing')
        return sorted(result, key=lambda item: item['key'])


def object_metadata(headers):
    return {k: v for k, v in headers.items() if k.startswith('x-amz-meta-') or k in
            ('content-type', 'content-disposition', 'content-encoding', 'cache-control', 'expires')}
