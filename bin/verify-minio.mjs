import { createHash, createHmac } from 'node:crypto'

// Read-only check: sign private S3 GETs, then compare them with the Java media
// response. Keys and hashes are safe to report; credentials are never printed.
const endpoint = new URL(process.env.MINIO_ENDPOINT || 'http://127.0.0.1:9000')
const api = process.env.VERIFY_API_BASE || 'http://127.0.0.1:8080'
const bucket = process.env.MINIO_BUCKET || 'jinse-wine'
const accessKey = process.env.MINIO_ACCESS_KEY || process.env.MINIO_ROOT_USER
const secretKey = process.env.MINIO_SECRET_KEY || process.env.MINIO_ROOT_PASSWORD
const region = process.env.MINIO_REGION || 'us-east-1'
if (!accessKey || !secretKey) throw new Error('Load MinIO credentials into the process environment first')
const hash = value => createHash('sha256').update(value).digest('hex')
const hmac = (key, value) => createHmac('sha256', key).update(value).digest()
const encode = value => encodeURIComponent(value).replace(/[!'()*]/g, char => `%${char.charCodeAt(0).toString(16).toUpperCase()}`)

async function privateObject(key) {
  const url = new URL(endpoint)
  url.pathname = `${endpoint.pathname.replace(/\/$/, '')}/${encode(bucket)}/${key.split('/').map(encode).join('/')}`
  const timestamp = new Date().toISOString().replace(/[:-]|\.\d{3}/g, '')
  const date = timestamp.slice(0, 8)
  const payload = hash('')
  const headers = { 'x-amz-date': timestamp, 'x-amz-content-sha256': payload }
  const signedHeaders = 'host;x-amz-content-sha256;x-amz-date'
  const canonicalHeaders = `host:${url.host}\nx-amz-content-sha256:${payload}\nx-amz-date:${timestamp}\n`
  const canonical = ['GET', url.pathname, '', canonicalHeaders, signedHeaders, payload].join('\n')
  const scope = `${date}/${region}/s3/aws4_request`
  const signingKey = hmac(hmac(hmac(hmac(`AWS4${secretKey}`, date), region), 's3'), 'aws4_request')
  const signature = hmac(signingKey, `AWS4-HMAC-SHA256\n${timestamp}\n${scope}\n${hash(canonical)}`).toString('hex')
  headers.Authorization = `AWS4-HMAC-SHA256 Credential=${accessKey}/${scope}, SignedHeaders=${signedHeaders}, Signature=${signature}`
  const response = await fetch(url, { headers, signal: AbortSignal.timeout(15000) })
  if (!response.ok) throw new Error(`MinIO object ${key}: HTTP ${response.status}`)
  return Buffer.from(await response.arrayBuffer())
}

const keys = process.argv.slice(2)
if (!keys.length) keys.push('mini-assets/design/store.jpg')
const results = []
for (const key of keys) {
  if (!/^(mini-assets|profile)\//.test(key) || key.includes('..') || key.includes('\\')) throw new Error('Invalid media key')
  const bytes = await privateObject(key)
  const response = await fetch(`${api.replace(/\/$/, '')}/${key}`, { signal: AbortSignal.timeout(15000) })
  if (!response.ok) throw new Error(`Java media ${key}: HTTP ${response.status}`)
  const served = Buffer.from(await response.arrayBuffer())
  if (!bytes.equals(served)) throw new Error(`MinIO and Java media differ: ${key}`)
  results.push({ key, bytes: bytes.length, sha256: hash(bytes), sameContent: true })
}
console.log(JSON.stringify({ bucket, objects: results }, null, 2))
