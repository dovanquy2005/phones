(function(){
  const path = location.pathname.split('/').pop() || 'index.html';
  const map = { 'products.html':'products', 'cart.html':'cart', 'help.html':'help', 'index.html':'products' };
  const key = map[path];
  if(key){ document.querySelectorAll(`.mainnav [data-nav="${key}"]`).forEach(a=>a.classList.add('is-active')); }
  const userName = window.CURRENT_USER?.name || localStorage.getItem('fs_user_name') || 'william shakespeare';
  const nameEl = document.getElementById('userName'); if(nameEl) nameEl.textContent = userName;
  const initials = (userName||'').split(/\s+/).filter(Boolean).map(s=>s[0]).slice(0,2).join('').toUpperCase() || 'ME';
  const av = document.getElementById('avatarCircle'); if(av) av.textContent = initials;
  const user = document.getElementById('userMenu');
  if(user){
    const btn = user.querySelector('.user__btn');
    btn.addEventListener('click', ()=>{
      const open = user.classList.toggle('is-open');
      btn.setAttribute('aria-expanded', open?'true':'false');
      const menu = user.querySelector('.user__menu'); if(menu) menu.setAttribute('aria-hidden', open?'false':'true');
    });
    document.addEventListener('click', (e)=>{ if(!user.contains(e.target)) user.classList.remove('is-open'); });
  }
})();