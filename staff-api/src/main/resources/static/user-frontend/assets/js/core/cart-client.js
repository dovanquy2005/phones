// assets/js/core/cart-client.js
// Robust cart client for frontend. Exposes window.FS_CartClient with methods:
// fetchServerCart(), addToCart(payloadOrSkuId, maybeQty), updateItem(itemId, qty), removeItem(itemId), clearCart()
// Emits events: 'cart:updated' (detail = cart object), 'fs:auth:unauthorized' (no detail)
// Expects backend AddItemRequest: { skuId: Long, qty: Int, name?, price?, imageUrl? }


import {} from './auth.js';

const API = window.FS_API || 'http://localhost:9090';
const LOCAL_KEY = 'fs_cart_local_v1';

// --- auth helpers ---------------------------------------------------------

function readToken() {
  try {
    let token = '';
    if (typeof window.getToken === 'function') token = window.getToken() || '';
    token = token || localStorage.getItem('fs_token') || localStorage.getItem('accessToken') || localStorage.getItem('access_token') || '';
    token = token.toString().trim();
    // loại bỏ "Bearer " nếu ai đó lưu kèm
    if (token.toLowerCase().startsWith('bearer ')) token = token.substring(7).trim();
    // nếu lưu vô tình "Bearer" (không có token) thì trả '' để không gửi header
    if (token.toLowerCase() === 'bearer') token = '';
    return token;
  } catch (e) {
    return '';
  }
}


function normalizeToken(t) {
  if (!t) return '';
  let s = String(t).trim();
  if (/^Bearer\s+/i.test(s)) s = s.replace(/^Bearer\s+/i, '').trim();
  // strip surrounding quotes
  if (/^".+"$/.test(s) || /^'.+'$/.test(s)) s = s.slice(1, -1);
  return s;
}
function authHeaders() {
  const token = readToken();
  return token ? { Authorization: 'Bearer ' + token } : {};
}

// --- events ----------------------------------------------------------------
function emitCart(cart) {
  try { window.dispatchEvent(new CustomEvent('cart:updated', { detail: cart })); } catch (e) { console.debug('emitCart error', e); }
}
async function handle401() {
  try {
    window.dispatchEvent(new CustomEvent('fs:auth:unauthorized'));
  } catch (e) {}
}

// --- local storage helpers -------------------------------------------------
function loadLocalCart() {
  try {
    const raw = localStorage.getItem(LOCAL_KEY);
    if (!raw) return { items: [], meta: {} };
    return JSON.parse(raw);
  } catch (e) {
    console.warn('loadLocalCart parse failed', e);
    return { items: [], meta: {} };
  }
}
function saveLocalCart(cart) {
  try { localStorage.setItem(LOCAL_KEY, JSON.stringify(cart)); } catch (e) {}
}

// --- network helpers ------------------------------------------------------
async function safeParseJson(res) {
  const txt = await res.text();
  try { return JSON.parse(txt); } catch (e) { return txt; }
}
async function postJson(url, body) {
  const headers = { 'Content-Type': 'application/json', ...authHeaders() };
  const res = await fetch(url, {
    method: 'POST',
    headers,
    credentials: 'include', // server uses cookies or same-origin sessions & CORS should allow
    body: JSON.stringify(body)
  });
  return res;
}

// --- API methods ----------------------------------------------------------
/**
 * fetchServerCart - GET /api/cart
 */
// async function fetchServerCart() {
//   try {
//     const headers = { 'Content-Type': 'application/json', ...authHeaders() };
//     const res = await fetch(`${API}/api/cart`, { method: 'GET', headers, credentials: 'include' });
//     if (res.status === 401) { await handle401(); return null; }
//     if (!res.ok) {
//       console.error('fetchServerCart failed', res.status, await res.text());
//       return null;
//     }
//     const cart = await res.json();
//     if (cart) { saveLocalCart(cart); emitCart(cart); }
//     return cart;
//   } catch (e) {
//     console.error('fetchServerCart exception', e);
//     const local = loadLocalCart();
//     emitCart(local);
//     return local;
//   }
// }

/**
 * addToCart(payloadOrSkuId, maybeQty)
 * Accepts:
 *  - addToCart({ skuId, qty, name?, price?, imageUrl? })  <-- preferred
 *  - addToCart(skuId, qty)                               <-- legacy (kept for compatibility)
 */
/**
 * Add to cart: strict input: { skuId: Number, qty: Number }
 * Returns { success: boolean, status?: number, error?: string, cart?: any }
 */
async function addToCart(input) {
  // must be plain object
  if (!input || typeof input !== 'object' || Array.isArray(input)) {
    console.warn('addToCart: input must be an object { skuId, qty }', input);
    return { success: false, status: 400, error: 'invalid input: object expected' };
  }

  // extract and validate fields
  const rawSku = input.skuId;
  const rawQty = input.qty;

  const skuId = (typeof rawSku === 'number' && Number.isFinite(rawSku)) ? rawSku
               : (typeof rawSku === 'string' && /^[0-9]+$/.test(rawSku.trim()) ? Number(rawSku.trim()) : null);
  const qty = Math.max(1, Number(rawQty || 1));

  if (!skuId || !Number.isFinite(skuId) || skuId <= 0) {
    console.warn('addToCart: missing/invalid skuId', input);
    return { success: false, status: 400, error: 'invalid skuId' };
  }

  // build body exactly as backend expects
  const body = { skuId: Number(skuId), qty: Number(qty) };
  console.error('body = ', body);
 
  try {
    // send to server
    const headers = { 'Content-Type': 'application/json', ...authHeaders() };
    const res = await postJson(`${API}/api/cart/items`, 
      body
    );

    if (res.status === 401) {
      await handle401();
      return { success: false, status: 401, error: 'unauthenticated' };
    }

    if (!res.ok) {
      const txt = await res.text().catch(()=>`${res.statusText || 'error'}`);
      
      console.error('addToCart failed', res.status, txt);
      // fallback to local behavior (keeps previous behavior)
      return fallbackAddLocal(body, txt || `HTTP ${res.status}`);
    }

    const cart = await safeParseJson(res);
    if (cart) {
      saveLocalCart(cart);
      emitCart(cart);
    }
    return { success: true, cart };
  } catch (e) {
    console.error('addToCart exception', e);
    return fallbackAddLocal(body, e && e.message ? e.message : 'network error');
  }
}


/**
 * updateItem(itemId, qty) -> PUT /api/cart/items/{itemId}
 */
async function updateItem(itemId, qty) {
  try {
    const headers = { 'Content-Type': 'application/json', ...authHeaders() };
    const res = await fetch(`${API}/api/cart/items/${encodeURIComponent(itemId)}`, {
      method: 'PUT',
      headers,
      credentials: 'include',
      body: JSON.stringify({ qty })
    });
    if (res.status === 401) { await handle401(); return { success: false, status: 401 }; }
    if (!res.ok) {
      const txt = await res.text();
      console.error('updateItem failed', res.status, txt);
      return { success: false, status: res.status, error: txt };
    }
    const cart = await safeParseJson(res);
    saveLocalCart(cart);
    emitCart(cart);
    return { success: true, cart };
  } catch (e) {
    console.error('updateItem exception', e);
    return { success: false, error: e.message };
  }
}

/**
 * removeItem(itemId) -> DELETE /api/cart/items/{itemId}
 */
async function removeItem(itemId) {
  try {
    const headers = { 'Content-Type': 'application/json', ...authHeaders() };
    const res = await fetch(`${API}/api/cart/items/${encodeURIComponent(itemId)}`, {
      method: 'DELETE',
      headers,
      credentials: 'include'
    });
    if (res.status === 401) { await handle401(); return { success: false, status: 401 }; }
    if (!res.ok) {
      const txt = await res.text();
      console.error('removeItem failed', res.status, txt);
      return { success: false, status: res.status, error: txt };
    }
    const cart = await safeParseJson(res);
    saveLocalCart(cart);
    emitCart(cart);
    return { success: true, cart };
  } catch (e) {
    console.error('removeItem exception', e);
    return { success: false, error: e.message };
  }
}

/**
 * clearCart -> POST /api/cart/clear
 */
async function clearCart() {
  try {
    const headers = { 'Content-Type': 'application/json', ...authHeaders() };
    const res = await fetch(`${API}/api/cart/clear`, { method: 'POST', headers, credentials: 'include' });
    if (res.status === 401) { await handle401(); return { success: false, status: 401 }; }
    if (!res.ok) {
      const txt = await res.text();
      console.error('clearCart failed', res.status, txt);
      return { success: false, status: res.status, error: txt };
    }
    const cart = await safeParseJson(res);
    saveLocalCart(cart);
    emitCart(cart);
    return { success: true, cart };
  } catch (e) {
    console.error('clearCart exception', e);
    return { success: false, error: e.message };
  }
}

// --- fallback / local helpers ---------------------------------------------
function fallbackAddLocal(body, reason) {
  try {
    const cart = loadLocalCart();
    // merge by skuId
    const key = String(body.skuId);
    const found = cart.items.find(it => String(it.skuId) === key);
    if (found) {
      found.qty = (Number(found.qty) || 0) + Number(body.qty || 1);
      if (body.name) found.name = body.name;
      if (body.price !== undefined) found.price = body.price;
      if (body.imageUrl) found.imageUrl = body.imageUrl;
    } else {
      cart.items.push({
        id: `local_${Date.now()}`,
        skuId: body.skuId,
        skuCode: body.skuCode || null,
        productId: body.productId || null,
        name: body.name || '',
        price: body.price || null,
        imageUrl: body.imageUrl || '',
        qty: Number(body.qty || 1)
      });
    }
    saveLocalCart(cart);
    emitCart(cart);
    console.warn('addToCart used local fallback because:', reason);
    return { success: true, cart, fallback: true };
  } catch (e) {
    console.error('fallbackAddLocal exception', e);
    return { success: false, error: e.message };
  }
}

/**
 * trySyncLocalToServer:
 * - Called after login. Sends local items to /api/cart/merge as { items: [ { skuId, qty, name?, price?, imageUrl? } ] }
 * - If merge succeeds, saved server cart replaces local.
 */
async function trySyncLocalToServer() {
  const token = readToken();
  if (!token) return;
  const local = loadLocalCart();
  if (!local || !Array.isArray(local.items) || local.items.length === 0) return;

  // build merge payload - only send skuId and qty + optional metadata
  const items = local.items.map(it => ({
    skuId: Number(it.skuId || it.sku || it.skuId),
    qty: Number(it.qty || 1),
    name: it.name || undefined,
    price: it.price !== undefined ? it.price : undefined,
    imageUrl: it.imageUrl || it.image || undefined
  })).filter(it => it.skuId && it.qty > 0);

  if (!items.length) return;

  try {
    const res = await postJson(`${API}/api/cart/merge`, { items });
    if (res.status === 401) { await handle401(); return; }
    if (!res.ok) {
      console.warn('trySyncLocalToServer: merge failed', res.status, await res.text());
      return;
    }
    const cart = await safeParseJson(res);
    saveLocalCart(cart);
    emitCart(cart);
  } catch (e) {
    console.warn('trySyncLocalToServer exception', e);
  }
}


// ----------------- Server cart + voucher helpers (paste into cart-client.js) -----------------

/**
 * fetchServerCart - GET /api/cart
 * - tries to fetch server-side cart (needs token or session)
 * - on 401 emits fs:auth:unauthorized via handle401()
 * - on success saves local copy and emits cart:updated
 */
// async function fetchServerCart() {
//   try {
//     const headers = { 'Content-Type': 'application/json', ...authHeaders() };
//     const res = await fetch(`${API}/api/cart`, { method: 'GET', headers, credentials: 'include' });

//     if (res.status === 401) {
//       await handle401();
//       return null;
//     }

//     if (!res.ok) {
//       const txt = await res.text().catch(() => res.statusText || 'error');
//       console.error('fetchServerCart failed', res.status, txt);
//       // fallback: emit local cart so UI still shows something
//       const local = loadLocalCart();
//       emitCart(local);
//       return local;
//     }

//     const cart = await safeParseJson(res);
//     if (cart) {
//       saveLocalCart(cart);
//       emitCart(cart);
//     }
//     return cart;
//   } catch (e) {
//     console.error('fetchServerCart exception', e);
//     const local = loadLocalCart();
//     emitCart(local);
//     return local;
//   }
// }

/**
 * applyVoucher(code) -> POST /api/cart/voucher/apply
 * returns { success: boolean, status?: number, error?: string, cart?: any, response?: any }
 */
async function applyVoucher(code) {
  if (!code || typeof code !== 'string' || !code.trim()) {
    return { success: false, status: 400, error: 'Mã rỗng' };
  }
  const body = { code: code.trim() };

  try {
    const res = await postJson(`${API}/api/cart/voucher/apply`, body);

    if (res.status === 401) {
      await handle401();
      return { success: false, status: 401, error: 'unauthenticated' };
    }

    if (!res.ok) {
      const txt = await res.text().catch(() => res.statusText || 'error');
      console.error('applyVoucher failed', res.status, txt);
      return { success: false, status: res.status, error: typeof txt === 'string' ? txt : JSON.stringify(txt) };
    }

    // server may return updated cart or a voucher response object
    const data = await safeParseJson(res);

    // prefer cart shape detection: if object and has 'items' or 'total' treat as cart
    let cart = null;
    if (data && typeof data === 'object') {
      if (Array.isArray(data.items) || data.total !== undefined || data.subtotal !== undefined) {
        cart = data;
      } else if (data.cart) {
        cart = data.cart;
      } else if (data.updatedCart) {
        cart = data.updatedCart;
      }
    }

    if (cart) {
      saveLocalCart(cart);
      emitCart(cart);
      return { success: true, cart, response: data };
    }

    // fallback: no cart returned but success
    return { success: true, response: data };
  } catch (e) {
    console.error('applyVoucher exception', e);
    return { success: false, error: e && e.message ? e.message : 'network error' };
  }
}

/**
 * removeVoucher() -> DELETE /api/cart/voucher
 * returns { success: boolean, status?: number, error?: string, cart?: any, response?: any }
 */
async function removeVoucher() {
  try {
    const headers = { 'Content-Type': 'application/json', ...authHeaders() };
    const res = await fetch(`${API}/api/cart/voucher`, { method: 'DELETE', headers, credentials: 'include' });

    if (res.status === 401) {
      await handle401();
      return { success: false, status: 401, error: 'unauthenticated' };
    }
    if (!res.ok) {
      const txt = await res.text().catch(() => res.statusText || 'error');
      console.error('removeVoucher failed', res.status, txt);
      return { success: false, status: res.status, error: typeof txt === 'string' ? txt : JSON.stringify(txt) };
    }

    const data = await safeParseJson(res);
    let cart = null;
    if (data && typeof data === 'object' && (Array.isArray(data.items) || data.total !== undefined || data.subtotal !== undefined)) {
      cart = data;
    } else if (data && data.cart) {
      cart = data.cart;
    }

    if (cart) {
      saveLocalCart(cart);
      emitCart(cart);
      return { success: true, cart, response: data };
    }

    return { success: true, response: data };
  } catch (e) {
    console.error('removeVoucher exception', e);
    return { success: false, error: e && e.message ? e.message : 'network error' };
  }
}

// ----------------- end of added helpers -----------------


// --- init & bindings ------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
  if (readToken()) {
    fetchServerCart();
    // small delay to allow other login handlers to run
    setTimeout(trySyncLocalToServer, 500);
  } else {
    emitCart(loadLocalCart());
  }
});

// refresh & sync on auth events
window.addEventListener('fs:auth:login', (e) => { fetchServerCart().then(() => trySyncLocalToServer()); });
window.addEventListener('fs:auth:logout', () => { const empty = { items: [], meta: {} }; saveLocalCart(empty); emitCart(empty); });

// --- expose API -----------------------------------------------------------
const FS_CartClient = {
  fetchServerCart,
  addToCart,
  updateItem,
  removeItem,
  clearCart,
  loadLocalCart,
  saveLocalCart,
  trySyncLocalToServer
};
try { window.FS_CartClient = FS_CartClient; } catch (e) {}
export default FS_CartClient;
