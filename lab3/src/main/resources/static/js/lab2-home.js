function sortHome(sort) {
  const params = new URLSearchParams(location.search);
  params.set('sort', ({recommend:'default',new:'newest',price:'price'})[sort]); params.delete('page');
  Market.go('/?' + params);
}
function filterHome(category) {
  const params = new URLSearchParams(location.search);
  const value = ({数码:'数码电子',影音:'影音设备',家居:'家居生活',办公:'办公学习'})[category];
  if (value) params.set('category', value); else params.delete('category');
  params.delete('page'); Market.go('/?' + params);
}
async function favorite(event, button) {
  event.stopPropagation(); if (!Market.requireLogin()) return;
  const enabled = !button.classList.contains('active'); button.disabled = true;
  try {
    const result = await Market.post('/api/favorite', { productId: button.dataset.id, enabled });
    button.classList.toggle('active', result.liked); button.setAttribute('aria-pressed', String(result.liked));
    button.querySelector('path')?.setAttribute('fill', result.liked ? 'currentColor' : 'none');
    Market.toast(result.liked ? '已加入收藏' : '已取消收藏');
  } catch (error) { Market.toast(error.message); } finally { button.disabled = false; }
}
const homeParams = new URLSearchParams(location.search);
document.getElementById('searchInput').value = homeParams.get('keyword') || '';
document.querySelectorAll('[data-sort]').forEach(button => button.classList.toggle('selected',
  ({recommend:'default',new:'newest',price:'price'})[button.dataset.sort] === (homeParams.get('sort') || 'default')));
document.querySelectorAll('[data-filter]').forEach(button => button.classList.toggle('active',
  ({全部:'',数码:'数码电子',影音:'影音设备',家居:'家居生活',办公:'办公学习'})[button.dataset.filter] === (homeParams.get('category') || '')));
