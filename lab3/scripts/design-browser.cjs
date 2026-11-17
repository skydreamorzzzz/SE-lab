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
async function productChecks(seller,buyer,admin,ids) {
 await compare(buyer,'商品展示','/products/'+ids[0],'detail',['.app','.topbar','.seller-card','.hero']);
 await buyer.locator('#commentContent').fill('设计验收：配件是否齐全？');await buyer.locator('#commentForm button').click();await buyer.waitForFunction(()=>document.querySelector('#commentList').textContent.includes('配件是否齐全'));
 await seller.goto(base+'/products/'+ids[0]);await seller.locator('[data-comment-reply] textarea').fill('配件齐全，支持当面检查');await seller.locator('[data-comment-reply] button').click();await seller.waitForFunction(()=>document.querySelector('#commentList').textContent.includes('支持当面检查'));
 await buyer.reload();await buyer.evaluate(()=>openModal('bargain'));await buyer.locator('#offer').fill('459');await buyer.locator('#offerMessage').fill('原型议价说明实际保存');await buyer.locator('#confirmBtn').click();await buyer.waitForURL(base+'/bargains');assert((await buyer.locator('body').innerText()).includes('原型议价说明实际保存'));
 await compare(seller,'出售商品','/seller/edit','publish',['.app','.top','.media-list','.intro']);
 await seller.locator('#samplePhoto').click();await seller.locator('#productName').fill('原版发布页真实商品');await seller.locator('#description').fill('通过原型图片网格、标签和保存草稿功能提交');await seller.locator('#minPrice').fill('30');await seller.locator('#maxPrice').fill('40');await seller.locator('#quantity').fill('2');await seller.locator('#saveTop').click();await seller.waitForFunction(()=>Boolean(document.querySelector('#publishData').dataset.id));const id=await seller.locator('#publishData').getAttribute('data-id');
 await seller.goto(base+'/seller/edit?id='+id);assert.equal(await seller.locator('#productName').inputValue(),'原版发布页真实商品');assert.equal(await seller.locator('.photo').count(),1);await seller.locator('#publishBtn').click();await seller.locator('#modalBack.show').waitFor();await post(admin,'/admin/'+id+'/audit',{approve:true,note:'发布页验收通过'});
 await buyer.goto(base+'/products/'+id);await buyer.evaluate(()=>openModal('buy'));await buyer.locator('#confirmBtn').click();await buyer.waitForURL(/\/orders\/\d+$/);const orderId=buyer.url().split('/').pop();await post(buyer,'/orders/'+orderId+'/pay',{});await post(seller,'/orders/'+orderId+'/ship',{});await post(buyer,'/orders/'+orderId+'/receive',{});await post(buyer,'/reviews/'+orderId,{rating:5,content:'原版购买弹窗交易完成'});
 checks.push('original detail/publish structures, persistent drafts, seller comment replies, quote message and complete real order flow');
}
async function workspaceChecks(seller,buyer,admin,ids){
 await compare(buyer,'用户资料','/profile','profile',['.app','.topbar','.profile-card','.shortcuts','.transactions','.bottom-nav']);
 await buyer.waitForFunction(()=>document.querySelector('[data-count=FAVORITE]').textContent==='1');await buyer.getByRole('button',{name:'详细资料',exact:false}).click();await buyer.locator('#profileEdit [name=city]').fill('南京 · 鼓楼');await buyer.locator('#profileEdit [name=bio]').fill('原型个人中心实际保存');await buyer.locator('#profileEdit button').click();await buyer.locator('#overlay').waitFor({state:'hidden'});await buyer.reload();assert((await buyer.locator('.identity-main').innerText()).includes('原型个人中心实际保存'));
 await buyer.getByRole('button',{name:'我的收藏',exact:false}).click();assert.equal(await buyer.locator('#modalBody .record').count(),1);await buyer.locator('.modal-close').click();await buyer.getByRole('button',{name:'消息通知',exact:true}).click();assert((await buyer.locator('#modalBody').innerText()).includes('订单'));await buyer.locator('.modal-close').click();
 await post(seller,'/seller/products/'+ids[1]+'/withdraw',{});await post(seller,'/seller/products/'+ids[1]+'/submit',{});
 await compare(admin,'管理员后台','/admin','admin',['.app','.topbar','.sidebar','.headline']);await admin.locator('.data-table tbody tr .action').first().click();await admin.getByRole('button',{name:'审核通过',exact:true}).click();await admin.waitForFunction(()=>document.querySelector('#pending-count').textContent==='0');await admin.locator('[data-tab=approved]').click();assert((await admin.locator('.data-table').innerText()).includes('无线降噪耳机'));
 await post(buyer,'/moderation/submit',{kind:'REPORT',type:'PRODUCT',target:ids[2],reason:'设计验收举报内容需核查'});await admin.reload();await admin.locator('[data-nav=reports]').click();await admin.locator('.data-table .action').first().click();await admin.getByRole('button',{name:'受理举报',exact:true}).click();await admin.locator('#reasonText').fill('核查完成，已记录反馈');await admin.getByRole('button',{name:'确认处理',exact:true}).click();await admin.waitForFunction(()=>document.querySelector('.data-table tbody').textContent.includes('已处理'));
 await admin.locator('[data-nav=violation]').click();const row=admin.locator('.data-table tbody tr').filter({hasText:'87 键机械键盘'});await row.locator('.action').click();await admin.getByRole('button',{name:'违规下架',exact:true}).click();await admin.locator('#reasonText').fill('验收违规下架，卖家可申诉');await admin.getByRole('button',{name:'确认下架',exact:true}).click();await admin.waitForFunction(()=>!document.querySelector('#drawer').classList.contains('show'));
 await post(seller,'/moderation/submit',{kind:'APPEAL',type:'PRODUCT',target:ids[2],reason:'卖家提供说明申请重新审核'});await admin.reload();await admin.locator('[data-nav=reports]').click();await admin.locator('.data-table tbody tr').filter({hasText:'违规申诉'}).locator('.action').click();await admin.getByRole('button',{name:'通过申诉',exact:true}).click();await admin.locator('#reasonText').fill('申诉通过，等待再次审核');await admin.getByRole('button',{name:'确认处理',exact:true}).click();await admin.waitForFunction(()=>!document.querySelector('#drawer').classList.contains('show'));
 await admin.locator('[data-nav=stats]').click();await admin.locator('#statPeriod').waitFor();assert((await admin.locator('#main').innerText()).includes('低样本合并'));await admin.locator('#statPeriod').selectOption('30');await admin.waitForFunction(()=>document.querySelectorAll('.bar-col').length===30);await admin.locator('[data-nav=users]').click();await admin.locator('.action').first().click();assert((await admin.locator('#drawerContent').innerText()).includes('买家 / 卖家'));
 const denied=await buyer.request.get(base+'/api/admin/dashboard');assert.equal(denied.status(),403);assert.equal((await buyer.request.post(base+'/api/profile',{form:{displayName:'unauthorized'}})).status(),403);
 checks.push('original profile cards/modals and admin sidebar/tabs/right drawer; real profile records, audit history, report feedback, punishment, appeal, private statistics and authorization');
}
async function run() {
  start();await ready();browser=await chromium.launch({executablePath:process.env.CHROME_PATH||'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true});
  const seller=await browser.newPage({viewport:{width:1280,height:1000}}),buyer=await browser.newPage({viewport:{width:1280,height:1000}}),admin=await browser.newPage({viewport:{width:1280,height:1000}});
  const errors=[];for(const page of [seller,buyer,admin])page.on('pageerror',e=>errors.push(e.message));
  await register(seller,'seller');await register(buyer,'buyer');await login(admin,'admin');const ids=await seed(seller,admin);
  await catalogChecks(seller,buyer,admin,ids);
  if(stage!=='catalog')await productChecks(seller,buyer,admin,ids);
  if(stage==='workspace'||stage==='full')await workspaceChecks(seller,buyer,admin,ids);
  assert.deepEqual(errors,[]);
  const result={passed:true,stage,checks,comparisons,consoleErrors:errors};fs.writeFileSync(path.join(output,'results.json'),JSON.stringify(result,null,2)+'\n');console.log(JSON.stringify(result,null,2));
}
run().catch(error=>{console.error(error);process.exitCode=1;}).finally(async()=>{if(browser)await browser.close();if(app&&app.exitCode===null){const stopped=new Promise(resolve=>app.once('close',resolve));app.kill();await stopped;}});
