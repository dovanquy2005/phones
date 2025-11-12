// assets/js/core/cart-guard.js — block anon cart writes & UI, NO auto-clear
;(function (win, doc) {
  const WRITE = /^(POST|PUT|PATCH|DELETE)$/i;
  const NEED_AUTH = /(\/|^)api\/.*(cart|basket|order\-items|cart\-items|line\-items|checkout)/i;
  const SKIP_PAGES = /\/(login\.html|register\.html|cart\.html|orders\.html|order.*\.html)(\?|#|$)/i;

  const getTokenSync = () => {
    try { return localStorage.getItem('fs_token') || sessionStorage.getItem('fs_token') || ''; }
    catch { return ''; }
  };
  const isLogged = () => !!getTokenSync();

  const unaccent = s => { try { return s.normalize('NFD').replace(/[\u0300-\u036f]/g,''); } catch { return s; } };
  function isAddToCartEl(el){
    if (!el) return false;
    if (el.matches('[data-add-to-cart],[data-action="add-to-cart"],.add-to-cart,.btn-add-to-cart,.btn-add-cart')) return true;
    const txt = unaccent((el.innerText || el.value || '').toLowerCase());
    const attrs = [
      el.id, el.name, el.className,
      el.getAttribute && (el.getAttribute('href')||''),
      el.dataset && (el.dataset.action||''), el.dataset && (el.dataset.role||'')
    ].join(' ').toLowerCase();
    const hitAttr = /\b(add|cart|gio|basket|mua|checkout)\b/.test(attrs);
    const hitText = /(them vao gio|them gio|add to cart|mua ngay)/.test(txt);
    return hitAttr || hitText;
  }

  function askLogin(){
    const go = confirm('Bạn cần đăng nhập để thêm vào giỏ hàng. Chuyển đến trang đăng nhập?');
    if (go) location.href = 'login.html?redirect=' + encodeURIComponent(location.href);
  }

  // UI guard (skip on cart/orders/login pages)
  if (!SKIP_PAGES.test(location.pathname)) {
    doc.addEventListener('click', (e)=>{
      const el = e.target.closest('button,a,input[type="submit"],[data-add-to-cart],[data-action="add-to-cart"],.add-to-cart,.btn-add-to-cart,.btn-add-cart');
      if (!el || !isAddToCartEl(el)) return;
      if (!isLogged()){ e.preventDefault(); e.stopPropagation(); askLogin(); }
    }, true);

    doc.addEventListener('submit', (e)=>{
      const f = e.target;
      const txt = unaccent((f.innerText||'').toLowerCase());
      const action = (f.getAttribute('action')||'').toLowerCase();
      if (/(cart|checkout|order\-items)/.test(action) || /(them vao gio|add to cart|mua ngay)/.test(txt)){
        if (!isLogged()){ e.preventDefault(); e.stopPropagation(); askLogin(); }
      }
    }, true);
  }

  // fetch guard (WRITE only)
  const _fetch = win.fetch.bind(win);
  win.fetch = async function(input, init){
    try{
      const u = (typeof input==='string') ? input : (input && input.url) || '';
      const m = ((init && init.method) || 'GET').toUpperCase();
      if (NEED_AUTH.test(u) && WRITE.test(m) && !isLogged()){
        askLogin(); throw new Error('Unauthorized cart write');
      }
    }catch(_){}
    return _fetch(input, init);
  };

  // XHR guard (WRITE only)
  const _open = XMLHttpRequest.prototype.open;
  XMLHttpRequest.prototype.open = function(m,u){ this.__m=(m||'GET').toUpperCase(); this.__u=String(u||''); return _open.apply(this, arguments); };
  const _send = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.send = function(body){
    try{
      if (WRITE.test(this.__m||'GET') && NEED_AUTH.test(this.__u||'') && !isLogged()){
        askLogin(); throw new Error('Unauthorized cart write (xhr)');
      }
    }catch(_){}
    return _send.apply(this, arguments);
  };

  // Block anonymous localStorage cart writes (chặn ghi giỏ khi chưa login)
  try{
    const S = Storage.prototype; const _setItem = S.setItem;
    S.setItem = function(key, value){
      try{
        if (!isLogged() && /^fs_cart$|^cart$|^shopping_cart$|^cartItems$/i.test(String(key))){
          askLogin(); return; // block write
        }
      }catch(_){}
      return _setItem.apply(this, arguments);
    };
  }catch(_){}

  // ❌ QUAN TRỌNG: KHÔNG tự xoá giỏ khi phát hiện có token nữa!
  // => Không clear local cart ở đây. Việc migrate/clear sẽ làm sau khi đăng nhập thành công (trong auth flow).
})(window, document);
