// Real Chromium smoke test. Uses an isolated H2 file under target and randomly generated credentials.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const root = path.resolve(__dirname, '..');
const output = path.join(root, 'target/browser-evidence');
fs.mkdirSync(output, { recursive: true });
const suffix = Date.now().toString(36);
const password = 'Run!9aA' + crypto.randomBytes(12).toString('hex');
const users = { admin: 'admin_' + suffix, seller: 'seller_' + suffix, buyer: 'buyer_' + suffix };
const url = 'http://127.0.0.1:18080';
let app;
let browser;
const checks = [];
function start() {
  const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin/java.exe') : 'java';
  const log = fs.openSync(path.join(output, 'application.log'), 'a');
  app = spawn(java, ['-jar', 'target/marketplace-1.0.0.jar', '--server.port=18080',
    '--spring.datasource.url=jdbc:h2:file:./target/browser-data/market-' + suffix], {
    cwd: root, windowsHide: true, stdio: ['ignore', log, log],
    env: { ...process.env, MARKET_ADMIN_USER: users.admin, MARKET_ADMIN_PASSWORD: password }
  });
}
async function ready() {
  for (let i = 0; i < 100; i++) {
    if (app.exitCode !== null) throw Error('Application exited, see application.log');
    try { const response = await fetch(url); if (response.ok) return; } catch {}
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  throw Error('Application startup timeout');
}
async function stop() {
  if (!app || app.exitCode !== null) return;
  const exited = new Promise(resolve => app.once('exit', resolve));
  app.kill();
  await exited;
}
async function text(page, expected) {
  assert((await page.locator('body').innerText()).includes(expected), 'Expected visible text: ' + expected);
}
async function login(page, role) {
  await page.goto(url + '/login');
  await page.locator('[name=username]').fill(users[role]);
  await page.locator('[name=password]').fill(password);
  await page.getByRole('button', { name: '登录', exact: true }).click();
  await page.waitForURL(url + '/');
}
async function register(page, role) {
  await page.goto(url + '/register');
  await page.locator('[name=username]').fill(users[role]);
  await page.locator('[name=password]').fill(password);
  await page.locator('[name=role]').selectOption(role.toUpperCase());
  await page.getByRole('button', { name: '注册', exact: true }).click();
  await text(page, '注册成功');
  await login(page, role);
}
async function run() {
  start();
  await ready();
  browser = await chromium.launch({ executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
  const seller = await browser.newPage({ viewport: { width: 1280, height: 1000 } });
  const buyer = await browser.newPage({ viewport: { width: 1280, height: 1000 } });
  const admin = await browser.newPage({ viewport: { width: 1280, height: 1000 } });
  await register(seller, 'seller');
  await register(buyer, 'buyer');
  await buyer.goto(url + '/profile');
  await buyer.locator('[name=displayName]').fill('浏览器验收买家');
  await buyer.locator('[name=address]').fill('南京 · 校园测试收货地址');
  await buyer.getByRole('button', { name: '保存资料' }).click();
  await login(admin, 'admin');
  await seller.goto(url + '/seller/edit');
  const fields = { name: '浏览器验收相机', category: '数码电子/相机', conditionLabel: '9成新', imageUrl: '/images/sample.svg', description: '真实表单联通验收，配件齐全', minPrice: '120.00', maxPrice: '150.00', reservationLimit: '5' };
  for (const [key, value] of Object.entries(fields)) await seller.locator('[name=' + key + ']').fill(value);
  await seller.getByRole('button', { name: '保存草稿 / 修改内容' }).click();
  await seller.locator('[name=delta]').fill('1');
  await seller.getByRole('button', { name: '调整库存' }).click();
  await seller.getByRole('button', { name: '提交审核', exact: true }).click();
  await admin.goto(url + '/admin');
  await admin.getByRole('button', { name: '通过', exact: true }).click();
  checks.push('register/login/profile/seller draft/stock/submit/admin approve');
  await buyer.goto(url + '/');
  await buyer.locator('[name=keyword]').fill('浏览器验收');
  await buyer.getByRole('button', { name: '搜索', exact: true }).click();
  await buyer.locator('.product').first().click();
  const productUrl = buyer.url();
  await buyer.getByRole('button', { name: '立即购买', exact: true }).click();
  await text(buyer, '待支付');
  const orderUrl = buyer.url();
  await buyer.getByRole('button', { name: '模拟支付', exact: true }).click();
  await seller.goto(orderUrl);
  await seller.getByRole('button', { name: '模拟发货', exact: true }).click();
  await buyer.goto(orderUrl);
  await buyer.getByRole('button', { name: '确认收货', exact: true }).click();
  await buyer.locator('[name=content]').fill('完整交易链路已通过浏览器验证');
  await buyer.getByRole('button', { name: '提交评价', exact: true }).click();
  await seller.goto(orderUrl);
  await seller.locator('[name=content]').fill('卖家评价已联通');
  await seller.getByRole('button', { name: '提交评价', exact: true }).click();
  await buyer.goto(productUrl);
  await text(buyer, '完整交易链路已通过浏览器验证');
  await buyer.screenshot({ path: path.join(output, 'product-reviews.png'), fullPage: true });
  checks.push('search/buy/simulate pay/ship/receive/both reviews');
  await buyer.getByRole('button', { name: '申请缺货预约', exact: true }).click();
  await buyer.getByRole('button', { name: '取消预约', exact: true }).click();
  await text(buyer, '已取消');
  await buyer.goto(productUrl);
  await buyer.getByRole('button', { name: '申请缺货预约', exact: true }).click();
  await seller.goto(url + '/seller/products');
  await seller.locator('[name=delta]').fill('1');
  await seller.getByRole('button', { name: '调整库存', exact: true }).click();
  await seller.goto(url + '/reservations');
  await seller.getByRole('button', { name: '确认已分配数量并创建订单', exact: true }).click();
  await text(seller, '待支付');
  checks.push('reserve/cancel/reserve again/restock/FIFO/seller confirmation/order');
  await admin.goto(url + '/admin/activities');
  await text(admin, 'CREATE');
  await admin.screenshot({ path: path.join(output, 'audit-log.png'), fullPage: true });
  const forbidden = await buyer.goto(url + '/admin');
  assert.equal(forbidden.status(), 403);
  const csrf = await buyer.request.post(url + '/orders/999/pay');
  assert.equal(csrf.status(), 403);
  checks.push('buyer denied admin and POST without CSRF denied');
  await buyer.goto(url + '/');
  await buyer.getByRole('button', { name: '退出', exact: true }).click();
  await text(buyer, '登录 / 注册');
  checks.push('logout');
  await stop();
  start();
  await ready();
  await login(buyer, 'buyer');
  await buyer.goto(orderUrl);
  await text(buyer, '已完成');
  await buyer.goto(productUrl);
  await text(buyer, '完整交易链路已通过浏览器验证');
  await buyer.getByRole('link', { name: '卖家主页与评价' }).click();
  await text(buyer, '5.0 / 5');
  await buyer.screenshot({ path: path.join(output, 'seller-profile.png'), fullPage: true });
  checks.push('process restart preserves accounts/order/reviews/seller reputation');
  fs.writeFileSync(path.join(output, 'results.json'), JSON.stringify({ passed: true, checks, productUrl, orderUrl }, null, 2));
  console.log(JSON.stringify({ passed: true, checks }, null, 2));
}
run().catch(error => { console.error(error); process.exitCode = 1; }).finally(async () => {
  if (browser) await browser.close();
  await stop();
});
