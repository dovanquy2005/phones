import { getCurrentUser, doLogout } from './auth.js';
(async () => {
  const user = await getCurrentUser();
  const root = document.querySelector('[data-header-user]') || document.querySelector('.auth-menu');
  if(!root) return;
  if(user){
    root.innerHTML = `<span class="u-name">${user.username || user.name || 'User'}</span>
      <a id="fs-logout" href="#" class="btn-link">Đăng xuất</a>`;
    document.getElementById('fs-logout')?.addEventListener('click', (e)=>{
      e.preventDefault(); doLogout();
    });
  }else{
    root.innerHTML = `<a href="login.html" class="btn-link">Đăng nhập</a>`;
  }
})();