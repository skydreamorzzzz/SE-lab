const $=id=>document.getElementById(id), initial=window.publishInitial||{}, root=$('publishData').dataset;
const photos=[initial.imageUrl,...String(initial.extraImages||'').split(/\s+/)].filter(Boolean);
let changed=false,busy=false;
const selections={category:(initial.category||'数码电子/相机').split('/')[0],condition:initial.condition||'9成新',extra:new Set(root.id?String(initial.tags||'').split(/\s+/).filter(Boolean):['可议价','支持邮寄'])};
if(initial.negotiable)selections.extra.add('可议价');
$('categoryDetail').value=(initial.category||'数码电子/相机').split('/')[1]||'其他';
function toast(message){Market.toast(message);}
function markChanged(){changed=true;}
function selectedTags(){for(const [id,key]of[['categoryTags','category'],['conditionTags','condition']])$(id).querySelectorAll('button').forEach(b=>b.classList.toggle('selected',b.dataset.value===selections[key]));$('extraTags').querySelectorAll('button').forEach(b=>b.classList.toggle('selected',selections.extra.has(b.dataset.value)));}
function selectSingle(id,key){$(id).onclick=event=>{const b=event.target.closest('button[data-value]');if(!b)return;selections[key]=b.dataset.value;selectedTags();markChanged();};}
selectSingle('categoryTags','category');selectSingle('conditionTags','condition');
$('extraTags').onclick=event=>{const b=event.target.closest('button[data-value]');if(!b)return;selections.extra.has(b.dataset.value)?selections.extra.delete(b.dataset.value):selections.extra.add(b.dataset.value);selectedTags();markChanged();};
function refreshPhotos(){document.querySelectorAll('.photo').forEach(el=>el.remove());photos.forEach((url,index)=>{const tile=document.createElement('div');tile.className='photo';const img=document.createElement('img');img.src=url;img.alt='商品照片 '+(index+1);const remove=document.createElement('button');remove.type='button';remove.textContent='×';remove.setAttribute('aria-label','删除第'+(index+1)+'张照片');remove.onclick=()=>{photos.splice(index,1);refreshPhotos();markChanged();};tile.append(img,remove);if(index===0){const cover=document.createElement('span');cover.className='cover-label';cover.textContent='封面';tile.append(cover);}$('mediaList').insertBefore(tile,$('addTile'));});$('photoCount').textContent=photos.length+' / 9 张';$('addTile').style.display=photos.length>=9?'none':'flex';}
async function upload(file){const form=new FormData();form.set('file',file);return (await Market.request('/api/media',{method:'POST',body:form})).url;}
$('photoInput').onchange=async event=>{if(busy)return;busy=true;$('uploadStatus').textContent='图片正在上传…';try{for(const file of event.target.files){if(photos.length>=9){toast('最多9张照片');break;}photos.push(await upload(file));refreshPhotos();markChanged();}$('uploadStatus').textContent='图片已上传，可保存到草稿后继续编辑。';}catch(error){$('uploadStatus').textContent=error.message;toast(error.message);}finally{event.target.value='';busy=false;}};
$('samplePhoto').onclick=()=>{if(photos.length<9){photos.push('/images/sample.svg');refreshPhotos();markChanged();}};
$('videoInput').onchange=async event=>{const file=event.target.files[0];if(!file)return;try{$('videoUrl').value=await upload(file);markChanged();toast('视频已上传');}catch(error){toast(error.message);}};
$('addTile').onkeydown=event=>{if(event.key==='Enter'||event.key===' '){event.preventDefault();$('photoInput').click();}};
$('description').oninput=()=>{$('charCount').textContent=$('description').value.length;markChanged();};
document.querySelectorAll('input,textarea').forEach(field=>field.addEventListener('input',markChanged));
function payload(publish){const data={name:$('productName').value.trim(),category:selections.category+'/'+($('categoryDetail').value.trim()||'其他'),conditionLabel:selections.condition,imageUrl:photos[0]||'',extraImages:photos.slice(1).join(' '),description:$('description').value.trim(),minPrice:$('minPrice').value,maxPrice:$('maxPrice').value,quantity:$('quantity').value||0,expectedStock:root.stock||0,expectedRevision:root.revision||0,publish,negotiable:selections.extra.has('可议价'),tradeTags:[...selections.extra].join(' '),returnSupported:$('returnSupported').checked,city:$('city').value.trim()};
 for(const name of ['reservationLimit','brand','modelName','usageYears','originPlace','contactChannel','shippingAddress','latitude','longitude','videoUrl'])if($(name).value!=='')data[name]=$(name).value;
 if(root.id)data.id=root.id;return data;}
async function save(publish){if(busy){toast('请等待上传或保存完成');return;}busy=true;$('publishBtn').disabled=true;$('saveTop').disabled=true;
 try{const data=payload(publish);if(publish && (!photos.length || !data.name || !data.description))throw Error('发布前请提供至少一张照片、名称与描述');
  const result=await Market.post('/api/seller/save',data);root.id=result.id;root.stock=result.stock;root.revision=result.revision;root.state=result.status;changed=false;
  if(publish){$('modalText').textContent=result.status==='PENDING_REVIEW'?'商品已提交审核，通过后才会公开展示。':'商品已保存，当前状态：'+result.status;$('modalBack').classList.add('show');}
  else toast('草稿已保存到服务器，重新打开页面仍可继续编辑');
 }catch(error){toast(error.message);}finally{busy=false;$('publishBtn').disabled=false;$('saveTop').disabled=false;}}
$('saveTop').onclick=()=>save(false);$('saveBottom').onclick=()=>save(false);$('publishBtn').onclick=()=>save(true);
$('modalStay').onclick=()=>Market.go('/seller/products');$('modalExit').onclick=()=>Market.go('/');
$('exitBtn').onclick=()=>{if(!changed||confirm('当前修改尚未保存，是否退出？'))Market.go('/profile');};
document.addEventListener('keydown',event=>{if(event.key==='Escape')$('modalBack').classList.remove('show');});
$('charCount').textContent=$('description').value.length;selectedTags();refreshPhotos();
