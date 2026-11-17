let profileData;
const e=Market.escape, recordKeys={'我的收藏':'FAVORITE','历史浏览':'VIEW','我的关注':'FOLLOW','我发布的':'LISTINGS','我卖出的':'SOLD','我买到的':'BOUGHT','消息通知':'MESSAGES'};
const glyph='<svg viewBox="0 0 24 24"><path d="M4 7h16v13H4zM8 7V4h8v3M9 13h6"/></svg>';
const row=(title,description,action,url)=>({title,description,action,url});
const shortcuts={
 '申请投诉':[row('举报与申诉','提交举报、查询反馈及申请复核','进入','/moderation')],
 '交易记录':[row('我发布的商品','草稿、审核、在售和下架','管理','/seller/products'),row('买卖订单','支付、发货、收货及退款评价','查看','/orders'),row('缺货预约','预约、取消、补货分配与确认','进入','/reservations'),row('议价记录','接受、拒绝和支付锁价订单','进入','/bargains')],
 '账户与安全':[row('手机号和找回凭据','绑定验证与生成恢复凭据','进入','/identity'),row('修改密码','验证原密码后修改登录密码','修改','#password')],
 '帮助中心':[row('交易流程','发布审核后可购买，模拟支付、发货、确认收货后双方评价','查看订单','/orders'),row('退款与投诉','已付款订单进入售后；违规处罚可通过申诉申请复核','进入','/moderation')],
 '设置':[row('个人资料','地区与简介对外公开；收货地址仅交易参与方可见','编辑','#profile'),row('屏蔽卖家','管理个人屏蔽关系','进入','/personal?kind=BLOCK'),row('消息通知','查看真实交易进度与处理反馈','查看','/messages')]
};
async function refreshProfile(){profileData=await Market.request('/api/profile');for(const c of document.querySelectorAll('[data-count]'))c.textContent=profileData.counts[c.dataset.count]||0;}
function formField(name,label,value,large=false){return '<label>'+label+(large?'<textarea':'<input')+' name="'+name+'" '+(name==='displayName'?'required maxlength="60"':name==='city'?'maxlength="40"':'maxlength="200"')+(large?'>'+e(value)+'</textarea>':' value="'+e(value)+'">')+'</label>';}
async function showModal(name){try{if(!profileData)await refreshProfile();document.getElementById('modalTitle').textContent=name;const target=document.getElementById('modalBody');document.getElementById('modalCaption').textContent='以下记录来自当前登录账户';
 if(name==='详细资料'||name==='收货地址'){
 const m=profileData.metric;target.innerHTML='<dl class="profile-fields"><dt>账户类型</dt><dd>'+e(profileData.role)+'</dd><dt>信誉评分</dt><dd>'+(m.reviewCount?m.score.toFixed(1)+' / 5.0':'暂无评价')+'</dd><dt>近期售出</dt><dd>'+m.recentSold+' 件（近30天）</dd></dl><form id="profileEdit" class="profile-editor">'+formField('displayName','昵称',profileData.name)+formField('city','公开地区',profileData.city)+formField('bio','公开个人简介',profileData.bio,true)+formField('address','收货地址（仅交易使用）',profileData.address,true)+'<button class="primary">保存资料</button></form>';
 }else if(name==='修改密码'){target.innerHTML='<form id="passwordEdit" class="profile-editor"><label>原密码<input name="oldPassword" type="password" autocomplete="current-password" required></label><label>新密码<input name="newPassword" type="password" autocomplete="new-password" required minlength="8"></label><small>至少8位，包含大小写字母、数字及特殊字符。</small><button class="primary">修改密码</button></form>';}
 else{const rows=recordKeys[name]?profileData.records[recordKeys[name]]:shortcuts[name]||[];target.innerHTML='<div class="records">'+(rows.length?rows.map(r=>'<a class="record" href="'+e(r.url)+'"><span class="record-ico">'+glyph+'</span><div class="record-main"><b>'+e(r.title)+'</b><p>'+e(r.description)+'</p></div><span class="record-state">'+e(r.action)+'</span></a>').join(''):'<p class="empty-note">暂无记录</p>')+'</div>';}
 const overlay=document.getElementById('overlay');overlay.classList.add('show');overlay.setAttribute('aria-hidden','false');
 }catch(error){Market.toast(error.message);}}
function closeModal(){const o=document.getElementById('overlay');o.classList.remove('show');o.setAttribute('aria-hidden','true');}
document.addEventListener('click',event=>{const link=event.target.closest('a[href="#password"],a[href="#profile"]');if(link){event.preventDefault();showModal(link.hash==='#password'?'修改密码':'详细资料');}});
document.addEventListener('submit',async event=>{if(!['profileEdit','passwordEdit'].includes(event.target.id))return;event.preventDefault();const values=Object.fromEntries(new FormData(event.target));if(event.target.id==='passwordEdit'){Market.submit('/profile/password',values);return;}try{await Market.post('/api/profile',values);await refreshProfile();document.querySelector('.identity-main h2>span').textContent=profileData.name;document.querySelector('.identity-main p').textContent=profileData.city||'尚未填写地区';document.querySelector('.identity-main .hint').textContent=profileData.bio||'让闲置找到更好的归宿 ✨';closeModal();Market.toast('资料已保存');}catch(error){Market.toast(error.message);}});
document.addEventListener('keydown',event=>{if(event.key==='Escape')closeModal();});
refreshProfile().catch(error=>Market.toast(error.message));
