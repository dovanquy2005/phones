// assets/js/core/cart-badge.js — improved: local-first, server-override, event-payload aware
(() => {
  const API = (window.FS_API || 'http://localhost:9090');

  const pickEl = () => document.querySelector('#navCart, .badge-cart');

  function setBadge(n){
    const a = pickEl(); if(!a) return;
    const base = a.getAttribute('data-label') || a.textContent.replace(/\s*\(\d+\)\s*$/,'').trim() || 'Giỏ hàng';
    a.setAttribute('data-label', base);
    a.textContent = n > 0 ? `${base} (${n})` : base;
  }

  function parseJSON(s){ try{ return JSON.parse(s); }catch{ return null; } }

  function countFromStorages(){
    const keys = ['fs_cart','cart','shopping_cart','cartItems'];
    let items = [];
    for(const S of [localStorage, sessionStorage]){
      for(const k of keys){
        try{
          const raw = S.getItem(k); if(!raw) continue;
          const v = parseJSON(raw);
          if(Array.isArray(v)) items = items.concat(v);
          else if(v && Array.isArray(v.items)) items = items.concat(v.items);
          else if(v && Array.isArray(v.lines)) items = items.concat(v.lines);
        }catch{}
      }
    }
    const totalQty = items.reduce((s,it)=> s + (Number(it.qty ?? it.quantity ?? 1) || 1), 0);
    return totalQty || items.length || 0;
  }

  async function countFromApi(){
    const headers = { 'Accept':'application/json' };
    try{
      const t = localStorage.getItem('fs_token') || sessionStorage.getItem('fs_token');
      if (t) headers['Authorization'] = 'Bearer ' + t;
    }catch{}
    try{
      const r = await fetch(`${API}/api/cart`, { headers, credentials: 'include' });
      if(!r.ok) return null;
      const data = await r.json();
      const arr = Array.isArray(data) ? data
                : Array.isArray(data.items) ? data.items
                : Array.isArray(data.lines) ? data.lines
                : Array.isArray(data.cartItems) ? data.cartItems : [];
      const totalQty = arr.reduce((s,it)=> s + (Number(it.qty ?? it.quantity ?? 1) || 1), 0);
      return totalQty || arr.length || 0;
    }catch(e){
      console.warn('cart-badge: countFromApi failed', e);
      return null;
    }
  }

  function debounce(fn, wait){
    let t = null;
    return (...args) => {
      clearTimeout(t);
      t = setTimeout(() => fn(...args), wait);
    };
  }

  async function initialRenderAndMaybeUpgrade(){
    const localCount = countFromStorages();
    setBadge(localCount);

    const apiCount = await countFromApi();
    if (apiCount != null && apiCount !== localCount) setBadge(apiCount);
  }

  const refreshFromApiDebounced = debounce(async () => {
    const apiCount = await countFromApi();
    if (apiCount != null) setBadge(apiCount);
  }, 300);

  // NEW: Listen auth and cart lifecycle events so badge always up-to-date
  window.addEventListener('cart:changed', (e) => {
    const d = e?.detail || {};
    if (typeof d.count === 'number'){
      setBadge(d.count);
      return;
    }
    setBadge(countFromStorages());
    const tkn = (localStorage.getItem('fs_token') || sessionStorage.getItem('fs_token') || '');
    if (tkn) refreshFromApiDebounced();
  });

  // react when login finishes or user info available
  window.addEventListener('auth:ready', () => { refreshFromApiDebounced(); });
  window.addEventListener('auth:login', () => { refreshFromApiDebounced(); });
  window.addEventListener('auth:logout', () => { // on logout prefer local (likely cleared)
    setBadge(countFromStorages());
  });

  // When local->server sync completes, serverCart provided
  window.addEventListener('cart:merged', (e) => {
    const serverCart = e?.detail?.serverCart;
    if (serverCart){
      const arr = Array.isArray(serverCart) ? serverCart
                : Array.isArray(serverCart.items) ? serverCart.items
                : Array.isArray(serverCart.lines) ? serverCart.lines : [];
      const total = arr.reduce((s,it)=> s + (Number(it.qty ?? it.quantity ?? 1) || 1), 0);
      if(total>0) { setBadge(total); return; }
    }
    // fallback refresh from API
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

  if (document.readyState === 'loading'){
    document.addEventListener('DOMContentLoaded', initialRenderAndMaybeUpgrade);
  } else {
    initialRenderAndMaybeUpgrade();
  }
})();
