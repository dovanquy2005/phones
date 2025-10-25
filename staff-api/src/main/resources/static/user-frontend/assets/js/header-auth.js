
(function(){
  const PATH = '/user-frontend';

  function fromLocal(){
    try{
      const raw = localStorage.getItem('fs_user');
      if (raw) return JSON.parse(raw);
    }catch{}
    const nm = localStorage.getItem('fs_user_name');
    const em = localStorage.getItem('fs_user_email');
    if (nm || em) return { name: nm, email: em };
    return null;
  }

  async function getUser(){
    const local = fromLocal();
    if (local) return local;
    try{
      const r = await fetch('/api/auth/me', { credentials:'include', headers:{'Accept':'application/json'} });
      if(!r.ok) return null; // 401/403 or others -> treat as guest
      return await r.json();
    }catch{ return null; }
  }

  function initials(name='?'){
    const a = name.trim().split(/\s+/).filter(Boolean);
    return ((a[0]?.[0]||'?') + (a[1]?.[0]||'')).toUpperCase();
  }

  function forceCartLink(){
    document.querySelectorAll('a').forEach(a=>{
      const txt=(a.textContent||'').trim().toLowerCase();
      const href=a.getAttribute('href')||'';
      if (txt==='giỏ hàng' || /(^|\/)cart\.html$/i.test(href)){
        a.setAttribute('href', PATH + '/cart.html');
      }
    });
  }

  function installUserMenu(user){
    const nav = document.querySelector('header .nav'); if(!nav) return;
    const loginLink = Array.from(nav.querySelectorAll('a')).find(a => (a.textContent||'').trim().toLowerCase()==='đăng nhập');
    if(!loginLink) return;
    const name = user?.name || user?.fullName || user?.username || 'Tài khoản';
    const wrap = document.createElement('div');
    wrap.className = 'auth-menu';
    wrap.innerHTML = `
      <button class="auth-avatar" type="button" aria-haspopup="menu" aria-expanded="false">
        <span class="avatar-dot">${initials(name)}</span>
        <span class="auth-name">${name}</span>
        <span class="auth-caret">▾</span>
      </button>
      <div class="auth-dropdown" role="menu" aria-hidden="true">
        <a role="menuitem" href="${PATH}/orders.html">Đơn hàng của tôi</a>
        <a role="menuitem" href="${PATH}/profile.html">Trang cá nhân</a>
        <div class="sep"></div>
        <a role="menuitem" id="logoutLink" href="${PATH}/login.html">Đăng xuất</a>
      </div>`;
    loginLink.replaceWith(wrap);
    const btn = wrap.querySelector('.auth-avatar');
    const dd  = wrap.querySelector('.auth-dropdown');
    btn.addEventListener('click', ()=>{
      const open = wrap.classList.toggle('open');
      btn.setAttribute('aria-expanded', open?'true':'false');
      dd.setAttribute('aria-hidden', open?'false':'true');
    });
    document.addEventListener('click', e=>{ if(!wrap.contains(e.target)) wrap.classList.remove('open'); });
    wrap.querySelector('#logoutLink').addEventListener('click', async (e)=>{
      e.preventDefault();
      try{ await fetch('/api/auth/logout', {method:'POST', credentials:'include'}); }catch{}
      localStorage.removeItem('fs_user');
      localStorage.removeItem('fs_user_name');
      localStorage.removeItem('fs_user_email');
      location.href = e.currentTarget.getAttribute('href');
    });
  }

  document.addEventListener('DOMContentLoaded', async ()=>{
    // make cart link robust
    forceCartLink();
    // upgrade header if logged in
    const me = await getUser();
    if (me) installUserMenu(me);
  });
})();
