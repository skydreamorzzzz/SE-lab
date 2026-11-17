const state = { keyword:'', minPrice:'', maxPrice:'', condition:'all', near:false, radius:5, sold:0, rating:0,
  sort:'recommend', page:0, latitude:null, longitude:null, category:new URLSearchParams(location.search).get('category') || '' };
let historyList = [], requestNumber = 0;
const byId = id => document.getElementById(id);
function updateSummary() {
  const parts = [];
  if(state.minPrice || state.maxPrice) parts.push('价格：¥' + (state.minPrice || '0') + ' – ¥' + (state.maxPrice || '不限'));
  if(state.condition !== 'all') parts.push('成色：' + state.condition);
  if(state.near) parts.push('附近 ' + state.radius + ' km');
  if(state.sold) parts.push('已卖 ≥ ' + state.sold);
  if(state.rating) parts.push('好评 ≥ ' + state.rating + '%');
  if(state.category) parts.push('分类：' + state.category);
  const root = byId('selectedSummary'); root.className = parts.length ? '' : 'selected-none'; root.replaceChildren();
  if (!parts.length) root.textContent = '尚未筛选，显示全部商品';
  for(const value of parts) { const el = document.createElement('span'); el.className='selected-value'; el.textContent=value; root.append(el); }
}
function historyRender() {
  const root = byId('historyList'); root.replaceChildren();
  if(!historyList.length) root.innerHTML='<span class="history-empty">暂无搜索记录</span>';
  for(const name of historyList) { const button=document.createElement('button'); button.type='button'; button.className='history-chip';
    button.textContent='⌕  ' + name; button.onclick=()=>{ byId('keyword').value=name; doSearch(true); }; root.append(button); }
}
async function loadHistory() {
  try { historyList = Market.authenticated() ? await Market.request('/api/search-history') : JSON.parse(sessionStorage.getItem('shiwu_guest_search_history') || '[]'); }
  catch { historyList=[]; } historyRender();
}
function setChips(group, name) { document.querySelectorAll('#'+group+' .chip').forEach(b=>{
  const active=b.dataset.value===String(state[name]); b.classList.toggle('active',active); b.setAttribute('aria-pressed',String(active)); }); }
function resultCard(p) {
  const e=Market.escape;
  const distance=p.distance == null ? '未提供定位' : p.distance.toFixed(1)+' km';
  const rating=p.reviewCount ? '好评 '+p.rating.toFixed(1)+'%' : '暂无评价';
  return `<article class="result" data-id="${p.id}" tabindex="0" role="link" onclick="Market.go('/products/${p.id}')" onkeydown="if(event.key==='Enter')this.click()">
    <div class="result-art"><img src="${e(p.imageUrl)}" alt="${e(p.name)}" loading="lazy"><span class="condition-sticker">${e(p.condition)}</span></div>
    <div class="result-text"><div class="result-title">${e(p.name)}</div><div class="result-sub">${e(p.tradeTags || p.description)}</div>
    <div class="result-metrics"><span>📍 ${distance}</span><span>已卖 ${p.sold} 件</span><span>${rating}</span></div>
    <div class="result-bottom"><strong>¥${e(p.price)}</strong><span>卖家 · ${e(p.city || p.sellerCity || p.seller)}</span></div></div></article>`;
}
async function doSearch(addHistory=false, page=0) {
  state.keyword=byId('keyword').value.trim(); state.page=page;
  if(state.minPrice && state.maxPrice && Number(state.minPrice)>Number(state.maxPrice)) { Market.toast('最高价格不能小于最低价格'); return; }
  const params=new URLSearchParams({keyword:state.keyword,category:state.category,condition:state.condition,
    sold:state.sold,rating:state.rating,page,sort:({recommend:'default',priceAsc:'price',priceDesc:'priceDesc',sold:'sales',rating:'positiveRate'})[state.sort],remember:addHistory});
  if(state.minPrice) params.set('min',state.minPrice); if(state.maxPrice) params.set('max',state.maxPrice);
  if(state.near) { params.set('radius',state.radius); params.set('latitude',state.latitude); params.set('longitude',state.longitude); }
  const thisRequest=++requestNumber;
  try {
    const data=await Market.request('/api/catalog?'+params); if(thisRequest!==requestNumber) return;
    byId('results').innerHTML=data.items.map(resultCard).join(''); byId('resultsPanel').hidden=false; byId('startHint').hidden=true;
    byId('noResults').hidden=data.total!==0; byId('resultHeading').textContent=state.keyword?'「'+state.keyword+'」的搜索结果':'筛选结果';
    byId('resultCount').textContent='找到 '+data.total+' 件符合条件的商品';
    byId('searchPages').hidden=data.page===0 && !data.hasNext; byId('previousPage').disabled=data.page===0;
    byId('nextPage').disabled=!data.hasNext; byId('pageLabel').textContent='第 '+(data.page+1)+' 页';
    if(addHistory && state.keyword) { if(Market.authenticated()) await loadHistory(); else {
      historyList=[state.keyword,...historyList.filter(x=>x!==state.keyword)].slice(0,8); sessionStorage.setItem('shiwu_guest_search_history',JSON.stringify(historyList)); historyRender(); } }
  } catch(error) { Market.toast(error.message); }
}
byId('searchForm').addEventListener('submit',event=>{event.preventDefault();doSearch(true);});
for(const [group,key] of [['conditionGroup','condition'],['soldGroup','sold'],['ratingGroup','rating']]) {
  byId(group).addEventListener('click',event=>{ const b=event.target.closest('[data-value]'); if(!b)return;
    state[key]=key==='condition'?b.dataset.value:Number(b.dataset.value); setChips(group,key); updateSummary(); doSearch(); });
}
for(const [id,key] of [['minPrice','minPrice'],['maxPrice','maxPrice']]) byId(id).onchange=event=>{state[key]=event.target.value;updateSummary();doSearch();};
byId('sortBy').onchange=event=>{state.sort=event.target.value;doSearch();};
byId('nearToggle').onclick=()=>{ if(state.latitude==null) {byId('locationDialog').showModal(); return;}
 state.near=!state.near;syncNear();doSearch(); };
function syncNear(){byId('nearToggle').classList.toggle('on',state.near);byId('nearToggle').setAttribute('aria-pressed',String(state.near));byId('nearRadius').disabled=!state.near;updateSummary();}
byId('nearRadius').onchange=event=>{state.radius=Number(event.target.value);updateSummary();doSearch();};
byId('setPosition').onclick=()=>byId('locationDialog').showModal();
byId('locateBrowser').onclick=()=>{ if(!navigator.geolocation){Market.toast('浏览器不支持定位，可手动填写坐标');return;}
 navigator.geolocation.getCurrentPosition(position=>{byId('viewerLatitude').value=position.coords.latitude;byId('viewerLongitude').value=position.coords.longitude;},()=>Market.toast('定位未获授权，可手动填写坐标')); };
byId('savePosition').onclick=()=>{ const lat=Number(byId('viewerLatitude').value),lon=Number(byId('viewerLongitude').value);
 if(!byId('viewerLatitude').value || !byId('viewerLongitude').value || !Number.isFinite(lat) || !Number.isFinite(lon) || Math.abs(lat)>90 || Math.abs(lon)>180){Market.toast('请输入合法的经纬度');return;}
 state.latitude=lat;state.longitude=lon;state.near=true;byId('locationStatus').textContent='已设置浏览位置';byId('locationDialog').close();syncNear();doSearch(); };
byId('resetFilters').onclick=()=>{Object.assign(state,{minPrice:'',maxPrice:'',condition:'all',near:false,radius:5,sold:0,rating:0,category:''});
 byId('minPrice').value='';byId('maxPrice').value='';byId('nearRadius').value='5';
 for(const [group,key] of [['conditionGroup','condition'],['soldGroup','sold'],['ratingGroup','rating']])setChips(group,key);syncNear();doSearch();};
byId('clearHistory').onclick=async()=>{try {if(Market.authenticated())await Market.post('/api/search-history/clear'); else sessionStorage.removeItem('shiwu_guest_search_history');historyList=[];historyRender();Market.toast('已清空搜索历史');}catch(error){Market.toast(error.message);}};
byId('previousPage').onclick=()=>doSearch(false,state.page-1);byId('nextPage').onclick=()=>doSearch(false,state.page+1);
loadHistory();updateSummary();
const params=new URLSearchParams(location.search);if(params.has('q') || params.has('keyword')) {byId('keyword').value=params.get('q') || params.get('keyword') || '';doSearch(true);}
if(params.has('nearby'))byId('locationDialog').showModal();
