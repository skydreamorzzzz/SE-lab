const productData=document.getElementById('productData').dataset;
let modalType='', liked=productData.favorite==='true';
function toast(message){Market.toast(message);}
function selectPhoto(button){document.querySelectorAll('.thumb').forEach(el=>el.classList.toggle('selected',el===button));document.getElementById('heroPhoto').src=button.dataset.url;document.querySelector('#hero .floating').textContent='实物展示 · '+(Number(button.dataset.index)+1)+' / '+document.querySelectorAll('.thumb').length;}
function favoriteState(){document.getElementById('favoriteBtn').classList.toggle('liked',liked);document.getElementById('heart')?.setAttribute('fill',liked?'currentColor':'none');document.getElementById('favLabel').textContent=liked?'已收藏':'收藏';}
async function toggleFavorite(){if(!Market.requireLogin())return;try{const response=await Market.post('/api/favorite',{productId:productData.id,enabled:!liked});liked=response.liked;favoriteState();toast(liked?'已加入收藏':'已取消收藏');}catch(error){toast(error.message);}}
function shareProduct(){navigator.clipboard?.writeText(location.href).then(()=>toast('已复制商品链接')).catch(()=>toast('请复制地址栏中的商品链接'));}
async function availability(){const response=await fetch('/availability/'+productData.id);if(!response.ok)throw Error('商品已下架或不可访问，请刷新');return response.json();}
async function openModal(type){
 if(!Market.requireLogin())return;
 if(productData.trader!=='true'){toast('管理员不能参与交易');return;}
 if(productData.owned==='true'){toast('不能购买或议价自己的商品');return;}
 try{const current=await availability();if(!['ON_SALE','SOLD_OUT'].includes(current.status))throw Error('商品当前不可交易');
   if(type==='bargain' && productData.negotiable!=='true')throw Error('此商品不支持议价');
   modalType=type==='buy' && current.status==='SOLD_OUT'?'reserve':type;
   document.getElementById('bargainFields').hidden=modalType!=='bargain';document.getElementById('buyFields').hidden=modalType==='bargain';
   document.getElementById('modalTitle').textContent=modalType==='bargain'?'向卖家发起砍价':modalType==='reserve'?'申请缺货预约':'确认购买信息';
   document.getElementById('modalDesc').textContent=modalType==='bargain'?'报价须位于 ¥'+productData.min+' – ¥'+productData.max+' 区间；卖家接受后锁价和库存15分钟。':modalType==='reserve'?'当前无现货，将按预约先后分配补货；可在个人中心取消预约。':'确认后创建待支付订单，15分钟内完成模拟支付；库存与价格将再次校验。';
   document.getElementById('confirmBtn').textContent=modalType==='bargain'?'发送砍价':modalType==='reserve'?'确认预约':'创建订单';
   document.getElementById('operationError').textContent='';document.getElementById('overlay').classList.add('open');document.body.style.overflow='hidden';updateTotal();
 }catch(error){toast(error.message);}
}
function closeModal(){document.getElementById('overlay').classList.remove('open');document.body.style.overflow='';}
function updateTotal(){document.getElementById('checkoutTotal').textContent=modalType==='reserve'?'补货后确认价格':'¥'+(Number(productData.price)*Number(document.getElementById('buyQuantity').value||1)).toFixed(2);}
document.getElementById('buyQuantity').oninput=updateTotal;
async function confirmModal(){const button=document.getElementById('confirmBtn');button.disabled=true;
 try{let result;const quantity=Number(document.getElementById(modalType==='bargain'?'offerQuantity':'buyQuantity').value);
  if(!Number.isInteger(quantity)||quantity<1)throw Error('数量必须是正整数');
  if(modalType==='bargain'){
    const price=document.getElementById('offer').value;
    if(!price||Number(price)<Number(productData.min)||Number(price)>Number(productData.max))throw Error('请输入商品价格区间内的报价');
    result=await Market.post('/api/products/'+productData.id+'/bargain',{quantity,price,message:document.getElementById('offerMessage').value});
  }else if(modalType==='reserve')result=await Market.post('/api/products/'+productData.id+'/reserve',{quantity});
  else{const current=await availability();if(current.status!=='ON_SALE'||current.stock<quantity||Number(current.price)!==Number(productData.price))throw Error('价格、库存或状态已变化，请刷新页面后下单');
    result=await Market.post('/api/products/'+productData.id+'/buy',{quantity,displayedPrice:productData.price,requestKey:productData.request});}
  Market.go(result.url);
 }catch(error){document.getElementById('operationError').textContent=error.message;}finally{button.disabled=false;}
}
function addToCart(){if(Market.requireLogin())Market.submit('/cart/add',{productId:productData.id,quantity:1,group:'默认'});}
let newestComments=false;
function sortComments(newest){newestComments=newest;document.querySelectorAll('.sort-comments [role=button]').forEach((element,index)=>element.style.color=(index===1)===newest?'#ff672b':'#969da6');loadComments();}
async function loadComments(){try{let comments=await Market.request('/api/products/'+productData.id+'/comments');if(!newestComments)comments=comments.slice().sort((a,b)=>Number(Boolean(b.reply))-Number(Boolean(a.reply)));const e=Market.escape;
 document.getElementById('commentCount').textContent='('+comments.length+')';document.getElementById('commentList').innerHTML=comments.length?comments.map(c=>`<div class="comment"><div class="comment-avatar">${e(c.author[0])}</div><div class="comment-body"><div class="comment-top"><b>${e(c.author)}</b><span>${new Date(c.createdAt).toLocaleDateString('zh-CN')}</span></div><p>${e(c.content)}</p>${c.reply?`<div class="comment-reply"><b>卖家回复：</b>${e(c.reply)}</div>`:''}${productData.owned==='true'?`<form data-comment-reply="${c.id}" class="comment-compose"><textarea aria-label="回复此留言" maxlength="1000" required placeholder="回复买家的问题">${e(c.reply||'')}</textarea><button class="reply-button">回复留言</button></form>`:''}</div></div>`).join(''):'<p class="empty-note">暂无留言，可以向卖家询问商品细节。</p>';
 }catch(error){toast(error.message);}}
document.getElementById('commentForm')?.addEventListener('submit',async event=>{event.preventDefault();const button=event.currentTarget.querySelector('button');button.disabled=true;try{await Market.post('/api/products/'+productData.id+'/comments',{content:document.getElementById('commentContent').value});document.getElementById('commentContent').value='';await loadComments();toast('留言已发布');}catch(error){toast(error.message);}finally{button.disabled=false;}});
document.addEventListener('submit',async event=>{if(!event.target.matches('[data-comment-reply]'))return;event.preventDefault();const form=event.target;try{await Market.post('/api/products/'+productData.id+'/reply',{commentId:form.dataset.commentReply,content:form.querySelector('textarea').value});await loadComments();toast('回复已保存');}catch(error){toast(error.message);}});
document.addEventListener('keydown',event=>{if(event.key==='Escape')closeModal();});
setInterval(async()=>{try{const current=await availability();document.getElementById('availabilityNote').textContent='实时状态 '+current.status+' / 可售库存 '+current.stock+' / 当前单价 ¥'+current.price+'；下单时仍会再次核验。';}catch(error){document.getElementById('availabilityNote').textContent=error.message;}},30000);
favoriteState();loadComments();
