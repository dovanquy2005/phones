(function(){
  const API_BASE = "http://localhost:9090";
  const FS = {
    getToken(){ try { return localStorage.getItem('fs_token') || sessionStorage.getItem('fs_token'); } catch { return null; } },
    getUser(){ try { return JSON.parse(localStorage.getItem('fs_user') || sessionStorage.getItem('fs_user') || '{}'); } catch { return {}; } },
    setAuth({token,user}, remember=false){
      const A = remember ? localStorage : sessionStorage;
      const B = remember ? sessionStorage : localStorage;
      if (token) A.setItem('fs_token', token);
      if (user)  A.setItem('fs_user', JSON.stringify(user));
      B.removeItem('fs_token'); B.removeItem('fs_user');
    },
    clear(){ try { localStorage.removeItem('fs_token'); localStorage.removeItem('fs_user'); sessionStorage.removeItem('fs_token'); sessionStorage.removeItem('fs_user'); } catch {} }
  };
  window.FS = FS;

  function initials(name, email){
    const s = (name || email || "U").trim();
    const parts = s.split(/\s+/);
    const i1 = parts[0]?.[0] || "U";
    const i2 = parts.length>1 ? parts[1][0] : "";
    return (i1 + i2).toUpperCase();
  }

  function renderHeader(){
    const nav = document.querySelector('header .nav');
    if (!nav) return;

    const token = FS.getToken();
    const user  = FS.getUser();
    // Remove old login link if exists
    Array.from(nav.querySelectorAll('a[href$="login.html"]')).forEach(a => a.remove());

    if (!token) {
      const a = document.createElement('a');
      a.href = '/user-frontend/login.html';
      a.textContent = 'Đăng nhập';
      nav.appendChild(a);
      return;
    }

    // Build user menu
    const name = user.fullName || user.name || user.email || 'Tài khoản';
    const inits = initials(user.fullName, user.email);
    const wrap = document.createElement('div');
    wrap.className = 'user-menu';
    wrap.innerHTML = `
      <button class="user-btn" id="userBtn" aria-haspopup="true" aria-expanded="false">
        <span class="user-avatar">${inits}</span>
        <span class="user-name">${name}</span>
        <svg width="16" height="16" viewBox="0 0 24 24" aria-hidden="true"><path d="M7 10l5 5 5-5z"/></svg>
      </button>
      <div class="user-dropdown" id="userDropdown">
        <a href="/user-frontend/profile.html">Chỉnh sửa thông tin</a>
        <a href="/user-frontend/orders.html">Lịch sử mua hàng</a>
        <div class="user-divider"></div>
        <button id="fsLogout">Đăng xuất</button>
      </div>
    `;
    nav.appendChild(wrap);

    const btn = wrap.querySelector('#userBtn');
    const dd  = wrap.querySelector('#userDropdown');
    const logout = wrap.querySelector('#fsLogout');

    function toggle(open){
      const isOpen = open ?? (dd.style.display !== 'block');
      dd.style.display = isOpen ? 'block' : 'none';
      btn.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
    }
    btn.addEventListener('click', (e)=>{ e.preventDefault(); e.stopPropagation(); toggle(); });
    document.addEventListener('click', ()=> toggle(false));
    dd.addEventListener('click', (e)=> e.stopPropagation());

    logout.addEventListener('click', ()=>{ FS.clear(); window.location.href = '/user-frontend/login.html'; });
  }

  document.addEventListener('DOMContentLoaded', renderHeader);
})();
