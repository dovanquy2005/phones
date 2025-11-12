// logout-handler.js (snippet)
import { getToken, clearAuth } from './core/auth.js'; // nếu dùng module
// or rely on globals window.getToken/window.clearAuth

async function handleLogout(evt) {
  evt && evt.preventDefault && evt.preventDefault();

  const API_BASE = window.FS_API || 'http://localhost:9090';
  const token = (typeof window.getToken === 'function') ? window.getToken() : (localStorage.getItem('fs_token') || sessionStorage.getItem('fs_token'));

  try {
    // Option A: call server clear cart while token still present
    if (token) {
      await fetch(API_BASE + '/api/cart/clear', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + token
        },
        credentials: 'include'
      }).catch(e => { /* ignore errors */ });
    }
  } catch (e) {
    // ignore network errors for best-effort
    console.warn('logout: server clear cart failed', e);
  }

  // Clear local client state (important)
  try {
    // Use same keys as auth.js / cart scripts
    const keys = ['fs_cart','cart','shopping_cart','cartItems','fs_user_id','fs_user','fs_token','accessToken','access_token'];
    keys.forEach(k => {
      try { localStorage.removeItem(k); } catch(_) {}
      try { sessionStorage.removeItem(k); } catch(_) {}
    });
  } catch (e) {}

  // Clear auth helpers (if available)
  try { if (typeof window.clearAuth === 'function') window.clearAuth(); } catch(e){}

  // Dispatch events so UI updates
  try {
    window.dispatchEvent(new CustomEvent('fs:auth:logout'));
    window.dispatchEvent(new CustomEvent('fs:cart:changed', { detail: { count: 0 } }));
  } catch (e) {}

  // Optional: reload page to be absolutely sure UI refreshed
  // window.location.href = '/';
}

// Example: attach to logout button
document.addEventListener('click', function (e) {
  const el = e.target.closest && e.target.closest('[data-action="logout"]');
  if (el) handleLogout(e);
});
