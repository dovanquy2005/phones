// assets/js/core/cart-badge.js
(() => {
  // Đảm bảo port đúng 9090 như bạn đang chạy
  const API = (window.FS_API || 'http://localhost:9090');

  const pickEl = () => document.querySelector('#navCart, .badge-cart');

  function setBadge(n) {
    const a = pickEl(); if (!a) return;
    const base = a.getAttribute('data-label') || a.textContent.replace(/\s*\(\d+\)\s*$/, '').trim() || 'Giỏ hàng';
    a.setAttribute('data-label', base);
    a.textContent = n > 0 ? `${base} (${n})` : base;
  }

  function parseJSON(s) { try { return JSON.parse(s); } catch { return null; } }

  // Đếm từ LocalStorage (Dành cho khách vãng lai hoặc khi chưa load xong API)
  function countFromStorages() {
    const keys = ['fs_cart', 'cart', 'shopping_cart', 'cartItems'];
    let items = [];
    for (const S of [localStorage, sessionStorage]) {
      for (const k of keys) {
        try {
          const raw = S.getItem(k); if (!raw) continue;
          const v = parseJSON(raw);
          if (Array.isArray(v)) items = items.concat(v);
          else if (v && Array.isArray(v.items)) items = items.concat(v.items);
          else if (v && Array.isArray(v.lines)) items = items.concat(v.lines);
        } catch { }
      }
    }
    const totalQty = items.reduce((s, it) => s + (Number(it.qty ?? it.quantity ?? 1) || 1), 0);
    return totalQty || items.length || 0;
  }

  // --- LOGIC ĐƯỢC SỬA Ở ĐÂY ---
  async function countFromApi() {
    const headers = { 'Accept': 'application/json' };
    let token = null;

    // 1. Lấy token an toàn
    try {
      token = localStorage.getItem('fs_token') || sessionStorage.getItem('fs_token');
    } catch { }

    // 2. QUAN TRỌNG: Nếu không có token, DỪNG LẠI NGAY.
    // Trả về null để code bên dưới biết là không lấy được dữ liệu từ server.
    if (!token) {
      return null; 
    }

    // 3. Nếu có token thì mới gắn vào Header và gọi API
    headers['Authorization'] = 'Bearer ' + token;

    try {
      const r = await fetch(`${API}/api/cart`, { headers });
      
      // Nếu token hết hạn (401) hoặc lỗi khác -> coi như không có dữ liệu
      if (!r.ok) return null; 

      const data = await r.json();
      const arr = Array.isArray(data) ? data
        : Array.isArray(data.items) ? data.items
        : Array.isArray(data.lines) ? data.lines
        : Array.isArray(data.cartItems) ? data.cartItems : [];
      
      const totalQty = arr.reduce((s, it) => s + (Number(it.qty ?? it.quantity ?? 1) || 1), 0);
      return totalQty || arr.length || 0;
    } catch (e) {
      console.warn('cart-badge: countFromApi failed', e);
      return null;
    }
  }

  function debounce(fn, wait) {
    let t = null;
    return (...args) => {
      clearTimeout(t);
      t = setTimeout(() => fn(...args), wait);
    };
  }

  async function initialRenderAndMaybeUpgrade() {
    // 1. Hiển thị ngay số lượng từ LocalStorage (để UI không bị giật)
    const localCount = countFromStorages();
    setBadge(localCount);

    // 2. Gọi API âm thầm kiểm tra
    const apiCount = await countFromApi();

    // 3. Chỉ cập nhật nếu API trả về kết quả hợp lệ (khác null) và khác số hiện tại
    if (apiCount != null && apiCount !== localCount) {
      setBadge(apiCount);
    }
  }

  const refreshFromApiDebounced = debounce(async () => {
    const apiCount = await countFromApi();
    if (apiCount != null) setBadge(apiCount);
  }, 300);

  // Lắng nghe các sự kiện thay đổi giỏ hàng
  window.addEventListener('cart:changed', (e) => {
    const d = e?.detail || {};
    if (typeof d.count === 'number') {
      setBadge(d.count);
      return;
    }
    // Fallback: tính lại từ storage và gọi API sync lại
    setBadge(countFromStorages());
    refreshFromApiDebounced();
  });

  // Khi đăng nhập thành công -> Gọi API ngay để lấy giỏ hàng của User đó
  window.addEventListener('auth:ready', () => { refreshFromApiDebounced(); });
  window.addEventListener('auth:login', () => { refreshFromApiDebounced(); });
  
  // Khi đăng xuất -> Quay về dùng LocalStorage (thường là 0 hoặc giỏ hàng rác)
  window.addEventListener('auth:logout', () => {
    setBadge(countFromStorages());
  });

  window.addEventListener('cart:merged', (e) => {
    const serverCart = e?.detail?.serverCart;
    if (serverCart) {
      const arr = Array.isArray(serverCart) ? serverCart
        : Array.isArray(serverCart.items) ? serverCart.items
        : Array.isArray(serverCart.lines) ? serverCart.lines : [];
      const total = arr.reduce((s, it) => s + (Number(it.qty ?? it.quantity ?? 1) || 1), 0);
      if (total > 0) { setBadge(total); return; }
    }
    refreshFromApiDebounced();
  });

  window.FS_CART_BADGE = {
    refresh: async () => {
      const local = countFromStorages();
      setBadge(local);
      const api = await countFromApi();
      if (api != null && api !== local) setBadge(api);
      return { local, api };
    },
    _countFromStorages: countFromStorages,
    _countFromApi: countFromApi
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initialRenderAndMaybeUpgrade);
  } else {
    initialRenderAndMaybeUpgrade();
  }
})();