// assets/js/core/header-auth.js (ESM, trimmed)
// Responsibilities:
// - Ensure cart link points to cart.html
// - Hide login link when token exists
import { getToken } from './auth.js';

function boot(){
  const nav = document.querySelector('.header .nav');
  if (!nav) return;

  // 1) Cart link -> cart.html
  const cartLink = nav.querySelector('#navCart, .badge-cart, a[href*="cart"]');
  if (cartLink) cartLink.setAttribute('href','cart.html');

  // 2) Hide login if logged-in (UI rendering handled by auth-menu-inject.js)
  try{
    const token = getToken && getToken();
    const loginLink = nav.querySelector('a[href$="login.html"]');
    if (token && loginLink) loginLink.style.display = 'none';
  }catch(_){/*noop*/}
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else { boot(); }


window.addEventListener('cart:updated', function(e){
  try { if(typeof renderHeaderCart === 'function') renderHeaderCart(); } catch(e){}
});
window.addEventListener('auth:changed', function(e){
  try { if(typeof renderHeaderCart === 'function') renderHeaderCart(); } catch(e){}
});
