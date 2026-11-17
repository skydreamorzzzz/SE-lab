window.Market = (() => {
  const escape = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const authenticated = () => document.querySelector('meta[name=authenticated]')?.content === 'true';
  const go = path => { window.location.href = path; };
  const toast = message => {
    const el = document.getElementById('toast');
    if (!el) return;
    el.textContent = message; el.classList.add('visible', 'show');
    clearTimeout(toast.timer); toast.timer = setTimeout(() => el.classList.remove('visible', 'show'), 2600);
  };
  async function request(path, options = {}) {
    const headers = new Headers(options.headers || {});
    headers.set('X-Requested-With', 'XMLHttpRequest');
    if (options.method && options.method !== 'GET') {
      const name = document.querySelector('meta[name=csrf-header]')?.content;
      const token = document.querySelector('meta[name=csrf]')?.content;
      if (name && token) headers.set(name, token);
    }
    const response = await fetch(path, { ...options, headers, credentials: 'same-origin' });
    if (new URL(response.url).pathname === '/login') { go('/login'); throw Error('请先登录'); }
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      const data = await response.json();
      if (!response.ok) throw Error(data.message || '请求未完成，请检查输入或权限');
      return data;
    }
    if (!response.ok) {
      const doc = new DOMParser().parseFromString(await response.text(), 'text/html');
      throw Error(doc.querySelector('.notice')?.textContent || '请求未完成，请检查输入或权限');
    }
    throw Error('服务器返回格式异常，请刷新后重试');
  }
  const post = (path, data = {}) => request(path, { method: 'POST', body: new URLSearchParams(data) });
  const requireLogin = () => { if (!authenticated()) { go('/login'); return false; } return true; };
  const submit = (path, data={}) => {
    const form=document.createElement('form');form.method='post';form.action=path;
    const fields={...data,_csrf:document.querySelector('meta[name=csrf]')?.content || document.querySelector('input[name=_csrf]')?.value};
    for(const [name,value] of Object.entries(fields)){const input=document.createElement('input');input.type='hidden';input.name=name;input.value=value;form.append(input);}
    document.body.append(form);form.submit();
  };
  return { escape, authenticated, requireLogin, go, toast, request, post, submit };
})();
