// assets/js/core/auth-menu-inject.js
;(function (win, doc) {
  const API = 'http://localhost:9090';

  function getToken() {
    try { return localStorage.getItem('fs_token') || sessionStorage.getItem('fs_token') || ''; }
    catch { return ''; }
  }
  function readUser() {
    try { return JSON.parse(localStorage.getItem('fs_user') || 'null'); } catch { return null; }
  }
  function writeUser(u) {
    try { if (u) localStorage.setItem('fs_user', JSON.stringify(u)); } catch {}
  }
  function normUser(u = {}) {
    return {
      id:    u.id || u.userId || u.uid || null,
      name:  u.name || u.fullName || u.fullname || u.displayName || u.username || u.email || 'User',
      email: u.email || null
    };
  }
  function initials(name) {
    if (!name) return 'U';
    const parts = String(name).trim().split(/\s+/).filter(Boolean);
    const take = (parts[0]?.[0] || '') + (parts[parts.length - 1]?.[0] || '');
    return (take || 'U').toUpperCase();
  }

  // ========== render/update ==========
  function ensureMenu(nav) {
    let wrap = nav.querySelector('.user-menu');
    if (!wrap) {
      wrap = doc.createElement('div');
      wrap.className = 'user-menu';
      wrap.innerHTML = `
        <button type="button" class="user-chip" aria-haspopup="true" aria-expanded="false">
          <span class="chip-avatar">U</span>
          <span class="chip-name">User</span>
          <span class="chip-caret" aria-hidden="true">▾</span>
        </button>
        <div class="user-dropdown" role="menu">
          <a role="menuitem" href="profile.html">Trang cá nhân</a>
          <a role="menuitem" href="orders.html">Đơn hàng của tôi</a>
          
          <div class="user-divider"></div>
          <button type="button" data-action="logout">Đăng xuất</button>
        </div>
      `;
      nav.appendChild(wrap);

      const btn  = wrap.querySelector('.user-chip');
      const drop = wrap.querySelector('.user-dropdown');

      function open(){ wrap.classList.add('open');  btn.setAttribute('aria-expanded','true'); }
      function close(){ wrap.classList.remove('open'); btn.setAttribute('aria-expanded','false'); }
      function toggle(e){ e?.preventDefault(); e?.stopPropagation(); wrap.classList.contains('open') ? close() : open(); }

      btn.addEventListener('click', toggle);
      btn.addEventListener('keydown', (e)=>{ if(e.key==='Enter'||e.key===' ') toggle(e); });
      doc.addEventListener('click', (e)=>{ if(!wrap.contains(e.target)) close(); });
      doc.addEventListener('keydown', (e)=>{ if(e.key==='Escape') close(); });

      // Xử lý sự kiện đăng xuất (Đã cập nhật để xóa sạch LocalStorage)
      drop.addEventListener('click', (e)=>{
        if(!e.target.closest('[data-action="logout"]')) return;
        e.preventDefault();
        try{
          // 1. Xóa sạch sessionStorage
          sessionStorage.clear(); 

          // 2. Xóa sạch các key quan trọng trong localStorage
          // (Xóa hết các biến thể tên gọi mà bạn từng dùng)
          const keysToRemove = [
            'fs_token', 'accessToken', 'access_token', 
            'fs_user', 'fs_user_id', 
            'fs_cart', 'fs_cart_local_v1', 
            'cart', 'shopping_cart'
          ];
          
          keysToRemove.forEach(key => localStorage.removeItem(key));

          // 3. (Tùy chọn) Nếu muốn xóa TRẮNG toàn bộ localStorage của domain này:
          // localStorage.clear(); 
        }catch{}
        
        // Chuyển hướng về trang đăng nhập
        location.href = 'login.html';
      });
    }
    return wrap;
  }

  function setName(wrap, nameText) {
    const nm = nameText && String(nameText).trim() ? String(nameText).trim() : 'User';
    const nameEl = wrap.querySelector('.chip-name');
    const avaEl  = wrap.querySelector('.chip-avatar');
    if (nameEl) nameEl.textContent = nm;
    if (avaEl)  avaEl.textContent  = initials(nm);
  }

  // ========== hydrate logic ==========
  async function fetchMeAndHydrate(wrap) {
    const token = getToken();
    if (!token) return;

    try {
      const r = await fetch(`${API}/api/auth/me`, { headers: { Authorization: 'Bearer ' + token } });
      if (!r.ok) return;
      const raw = await r.json().catch(()=>null);
      const me = normUser(raw && (raw.user || raw));
      if (me) {
        writeUser(me);         // lưu để các trang sau dùng
        setName(wrap, me.name);  // cập nhật ngay, không cần F5
      }
    } catch {}
  }

  function init() {
    const nav = doc.querySelector('.header .nav');
    if (!nav) return;
    if (!getToken()) return;        // chỉ hiện khi đã login

    const wrap = ensureMenu(nav);

    // 1) set tên ban đầu từ fs_user (nếu chưa có sẽ là "User")
    const u0 = readUser();
    if (u0) setName(wrap, normUser(u0).name);

    // 2) hydrate bằng /api/auth/me (sẽ cập nhật lại tên khi có)
    fetchMeAndHydrate(wrap);

    // 3) nếu nơi khác cập nhật fs_user -> cập nhật UI
    win.addEventListener('storage', (e)=>{
      if (e.key === 'fs_user') {
        try { const u = JSON.parse(e.newValue || 'null'); setName(wrap, normUser(u).name); } catch {}
      }
    });

    // 4) fallback: sau 1s kiểm tra lại fs_user một lần nữa
    setTimeout(()=>{
      const u = readUser();
      if (u) setName(wrap, normUser(u).name);
    }, 1000);
  }

  if (doc.readyState === 'loading') doc.addEventListener('DOMContentLoaded', init); else init();
})(window, document);