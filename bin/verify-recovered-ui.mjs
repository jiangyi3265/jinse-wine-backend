import assert from 'node:assert/strict'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const { chromium } = await import(pathToFileURL(process.env.RECOVERY_PLAYWRIGHT_MODULE).href)
const api = process.env.RECOVERY_API
const adminOrigin = process.env.RECOVERY_ADMIN_ORIGIN
const h5Origin = process.env.RECOVERY_H5_ORIGIN
const output = process.env.RECOVERY_UI_OUTPUT
const browser = await chromium.launch({ channel: 'msedge', headless: true })
const evidence = []
try {
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
  const errors = []
  const external = []
  await context.route('**/*', async route => {
    const url = new URL(route.request().url())
    if (url.origin === 'https://jiuyekl.oksja.cn') {
      const response = await route.fetch({ url: api + url.pathname + url.search })
      return route.fulfill({ response, headers: { ...response.headers(), 'access-control-allow-origin': '*' } })
    }
    if (url.origin === adminOrigin || url.origin === h5Origin || url.origin === api || url.protocol === 'data:') return route.continue()
    external.push(url.origin)
    await route.abort()
  })
  await context.addCookies([{ name: 'Admin-Token', value: process.env.RECOVERY_ADMIN_TOKEN, url: adminOrigin }])
  await context.addInitScript(token => localStorage.setItem('jinse-mini-token-v1', JSON.stringify({ type: 'string', data: token })), process.env.RECOVERY_MEMBER_TOKEN)
  const page = await context.newPage()
  page.on('pageerror', error => errors.push(error.message))
  async function inspect(name) {
    await page.waitForFunction(() => [...document.querySelectorAll('img')].filter(img => img.getAttribute('src')).every(img => img.complete && img.naturalWidth > 0))
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), name)
    assert.ok((await page.locator('body').innerText()).trim().length > 20, name)
    const filename = name + '.png'
    await page.screenshot({ path: path.join(output, filename), fullPage: true })
    evidence.push({ name, viewport: page.viewportSize(), screenshot: filename, imagesLoaded: true, noHorizontalOverflow: true })
  }
  await page.goto(adminOrigin + '/index', { waitUntil: 'networkidle' })
  await page.locator('.sidebar-container').waitFor()
  assert.equal(await page.locator('.sidebar-container').getByText('小程序内容', { exact: true }).count(), 0)
  await inspect('recovered-admin-home-desktop')
  await page.goto(adminOrigin + '/business/report', { waitUntil: 'networkidle' })
  await page.getByText('销售订单', { exact: true }).first().waitFor()
  await inspect('recovered-admin-finance-desktop')
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto(h5Origin + '/#/pages/shop/index', { waitUntil: 'networkidle' })
  await page.locator('.product-card').first().waitFor()
  await inspect('recovered-mini-shop-mobile')
  await page.goto(h5Origin + '/#/pages/orders/index', { waitUntil: 'networkidle' })
  await page.locator('.order-card').first().waitFor()
  await inspect('recovered-mini-orders-mobile')
  assert.deepEqual(errors, [])
  assert.deepEqual(external, [])
  console.log(JSON.stringify({ views: evidence, pageErrors: errors, unexpectedExternalRequests: external, usesRealRecoveredApi: true }))
} finally { await browser.close() }
