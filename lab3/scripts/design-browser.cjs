const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const stage = process.argv.find(a => a.startsWith('--stage='))?.split('=')[1] || 'all';
const output = path.join(root, 'target/design-validation', Date.now().toString(36));
fs.mkdirSync(output, { recursive: true });
const source = path.resolve(root, '../../过去的lab实验/lab2/实验2_241220086_唐天洋/UI界面');
const suffix = Date.now().toString(36), password = 'Design!9aA' + crypto.randomBytes(10).toString('hex');
const users = { seller:'designseller_' + suffix, buyer:'designbuyer_' + suffix, admin:'designadmin_' + suffix, super:'designsuper_' + suffix };
const base = 'http://127.0.0.1:18080';
const checks = [], comparisons = [];
let app, browser;
function start() {
  const jar = path.join(output, 'application.jar');
  fs.copyFileSync(path.join(root, 'target/marketplace-1.0.0.jar'), jar);
  const descriptor = fs.openSync(path.join(output, 'application.log'), 'a');
  const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin/java.exe') : 'java';
  app = spawn(java, ['-jar', jar, '--server.port=18080', '--spring.datasource.url=jdbc:h2:file:' + path.join(output, 'database') + ';WRITE_DELAY=0',
    '--market.media-dir=' + path.join(output, 'media'), '--market.backup-dir=' + path.join(output, 'backups')], {
    cwd:root, windowsHide:true, stdio:['ignore',descriptor,descriptor],
    env:{...process.env, MARKET_ADMIN_USER:users.admin, MARKET_ADMIN_PASSWORD:password, MARKET_SUPER_ADMIN_USER:users.super, MARKET_SUPER_ADMIN_PASSWORD:password}
  });
  fs.closeSync(descriptor);
}
async function ready() {
  for(let i=0;i<100;i++) {
    if(app.exitCode!==null) throw Error('Application exited; inspect ' + path.join(output,'application.log'));
    try { const response=await fetch(base);if(response.ok)return; }catch{}
    await new Promise(resolve=>setTimeout(resolve,500));
  }
  throw Error('Application did not start');
}
async function token(page) { return await page.locator('meta[name=csrf]').count() ? page.locator('meta[name=csrf]').getAttribute('content') : page.locator('input[name=_csrf]').first().getAttribute('value'); }
async function post(page, url, data) {
  const response=await page.request.post(base+url,{form:{...data,_csrf:await token(page)}});
  assert(response.ok(), 'POST ' + url + ' status ' + response.status());return response;
}
async function login(page, role) {
  await page.goto(base+'/login'); await page.locator('[name=username]').fill(users[role]); await page.locator('[name=password]').fill(password);
  await page.getByRole('button',{name:'登录',exact:true}).click();await page.waitForURL(base+'/');
}
async function register(page, role) {
  await page.goto(base+'/register'); await post(page,'/register',{username:users[role],password,role:role.toUpperCase()});await login(page,role);
  await post(page,'/profile',{displayName:role==='seller'?'橙子同学':'设计验收买家',address:'南京校园验收收货点'});
}
async function rects(page, selectors) {
  return page.evaluate(selectors=>Object.fromEntries(selectors.map(selector=>{
    const e=document.querySelector(selector);if(!e)return[selector,null];const r=e.getBoundingClientRect();return[selector,{x:r.x,y:r.y,width:r.width,height:r.height}];
  })),selectors);
}
async function compare(page, original, actual, name, selectors) {
  const reference=await browser.newPage({viewport:{width:1280,height:1000}});
  await reference.goto(pathToFileURL(path.join(source,original+'.html')).href);await reference.evaluate(()=>document.fonts.ready);
  await reference.screenshot({path:path.join(output,name+'-reference.png'),fullPage:true});const expected=await rects(reference,selectors);
  await page.goto(base+actual);await page.evaluate(()=>document.fonts.ready);await page.screenshot({path:path.join(output,name+'-actual.png'),fullPage:true});
  const observed=await rects(page,selectors);let maxDifference=0;
  for(const selector of selectors) { assert(expected[selector]&&observed[selector], 'Missing original component '+selector);for(const key of ['x','y','width','height'])maxDifference=Math.max(maxDifference,Math.abs(expected[selector][key]-observed[selector][key])); }
  comparisons.push({page:name,original:original+'.html',selectors,maxGeometryDifferencePx:maxDifference});await reference.close();
}
async function seed(seller,admin) {
  const fixtures=[['复古数码相机 · 九成新','数码电子/相机','camera','459','9成新','功能完好 / 附原装电池和背带'],['无线降噪耳机 · 头戴式','影音设备/耳机','headphones','268','近全新','音质清晰 / 有收纳袋 / 可小刀'],['87 键机械键盘 · 奶白色','办公学习/键盘','keyboard','129','9成新','全键无冲 / 送防尘罩 / 自提优先'],['奶油风落地灯 · 暖光','家居生活/灯具','lamp','86','8成新','房间氛围感 / 正常使用 / 可同城']];
  const ids=[];
  // Reverse insertion reproduces the prototype's default card order under stable newest-first sorting.
  for(const [name,category,image,price,conditionLabel,description] of fixtures.slice().reverse()) {
    const response=await post(seller,'/seller/save',{name,category,imageUrl:'/images/lab2/'+image+'.svg',minPrice:price,maxPrice:price,conditionLabel,description,reservationLimit:8,negotiable:true,city:'南京',latitude:32.0,longitude:118.0,tradeTags:'可议价 支持邮寄'});
    const html=await response.text();const id=html.match(/href="\/products\/(\d+)"/)[1];
    await post(seller,`/seller/products/${id}/stock`,{delta:3});await post(seller,`/seller/products/${id}/submit`,{});await post(admin,`/admin/${id}/audit`,{approve:true,note:'设计验收审核通过'});ids.unshift(Number(id));
  }
  return ids;
}
async function catalogChecks(seller,buyer,admin,ids) {
  await compare(buyer,'主页','/','home',['.app','.top','.browse-row','.filters','.bottom-nav']);
  assert.equal(await buyer.locator('.product').count(),4);
  await buyer.locator('.product').first().locator('.like').click();await buyer.waitForFunction(()=>document.querySelector('.product .like').classList.contains('active'));
  await buyer.reload();assert(await buyer.locator('.product .like').first().getAttribute('class').then(x=>x.includes('active')));
  await compare(buyer,'搜索页面','/search','search',['.app','.top','.filter-section','.bottom-nav']);
  await buyer.locator('#keyword').fill('相机');await buyer.getByRole('button',{name:'搜索商品',exact:true}).click();await buyer.waitForFunction(()=>document.querySelector('#resultCount').textContent.includes('1 件'));
  assert.equal(await buyer.locator('#results .result').count(),1);
  await buyer.locator('#keyword').fill('');await buyer.getByRole('button',{name:'搜索商品',exact:true}).click();await buyer.waitForFunction(()=>document.querySelectorAll('#results .result').length===4);
  await buyer.locator('#minPrice').fill('200');await buyer.locator('#minPrice').dispatchEvent('change');await buyer.waitForFunction(()=>document.querySelectorAll('#results .result').length===2);
  await buyer.locator('#conditionGroup [data-value="9成新"]').click();await buyer.waitForFunction(()=>document.querySelectorAll('#results .result').length===1);
  await buyer.locator('#resetFilters').click();await buyer.waitForFunction(()=>document.querySelectorAll('#results .result').length===4);
  await buyer.locator('#nearToggle').click();await buyer.locator('#viewerLatitude').fill('32.0');await buyer.locator('#viewerLongitude').fill('118.0');await buyer.locator('#savePosition').click();await buyer.waitForFunction(()=>document.querySelector('#selectedSummary').textContent.includes('附近'));
  await buyer.locator('#sortBy').selectOption('rating');await buyer.waitForTimeout(200);
  const response=await buyer.request.get(base+'/api/catalog');const json=await response.text();assert(!json.includes('"passwordHash":')&&!json.includes('"recoveryHash":')&&!json.includes('"phone":'));
  checks.push('original home/search structure, real filtering, favorite persistence, nearby coordinates and safe public DTOs');
}
async function run() {
  start();await ready();browser=await chromium.launch({executablePath:process.env.CHROME_PATH||'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true});
  const seller=await browser.newPage({viewport:{width:1280,height:1000}}),buyer=await browser.newPage({viewport:{width:1280,height:1000}}),admin=await browser.newPage({viewport:{width:1280,height:1000}});
  const errors=[];for(const page of [seller,buyer,admin])page.on('pageerror',e=>errors.push(e.message));
  await register(seller,'seller');await register(buyer,'buyer');await login(admin,'admin');const ids=await seed(seller,admin);
  await catalogChecks(seller,buyer,admin,ids);
  assert.deepEqual(errors,[]);
  const result={passed:true,stage,checks,comparisons,consoleErrors:errors};fs.writeFileSync(path.join(output,'results.json'),JSON.stringify(result,null,2)+'\n');console.log(JSON.stringify(result,null,2));
}
run().catch(error=>{console.error(error);process.exitCode=1;}).finally(async()=>{if(browser)await browser.close();if(app&&app.exitCode===null){const stopped=new Promise(resolve=>app.once('close',resolve));app.kill();await stopped;}});
