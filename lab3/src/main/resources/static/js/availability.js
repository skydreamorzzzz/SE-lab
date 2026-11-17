const panel = document.querySelector('[data-availability]');
if (panel) {
  const refresh = async () => {
    const response = await fetch(panel.dataset.availability, { headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error('商品状态变化或不可访问，请刷新');
    return response.json();
  };
  setInterval(async () => {
    try {
      const data = await refresh();
      panel.textContent = `实时状态 ${data.status} / 可售库存 ${data.stock} / 单价 ¥${data.price}`;
    } catch (error) { panel.textContent = error.message; }
  }, 30000);
  const buy = document.querySelector('form[action^="/buy/"]');
  if (buy) buy.addEventListener('submit', async event => {
    if (buy.dataset.checked === 'yes') return;
    event.preventDefault();
    try {
      const data = await refresh();
      if (data.status !== 'ON_SALE' || data.stock < Number(buy.quantity.value)
          || Number(data.price) !== Number(buy.displayedPrice.value)) {
        panel.textContent = '价格、库存或状态发生变化，请刷新页面后购买';
        return;
      }
      buy.dataset.checked = 'yes';
      buy.requestSubmit();
    } catch (error) { panel.textContent = error.message; }
  });
}
