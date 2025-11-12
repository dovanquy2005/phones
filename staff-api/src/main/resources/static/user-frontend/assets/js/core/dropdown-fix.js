;(function(win,doc){
  function setup(container){
    if(!container || container.classList.contains('user-menu')) return;
    let trigger = container.querySelector('[data-menu-toggle]') || container.querySelector('.user-trigger') || container.querySelector('#authName')?.parentElement;
    let menu = container.querySelector('.user-dropdown') || container.querySelector('.menu') || container.querySelector('ul[role="menu"]');
    if(!trigger){
      trigger = container.querySelector('[data-auth-trigger]') || container.firstElementChild;
    }
    if(!menu && trigger){
      let el = trigger.nextElementSibling;
      while(el && !(el.tagName==='UL' || el.classList.contains('menu') || el.classList.contains('user-dropdown'))){
        el = el.nextElementSibling;
      }
      menu = el;
    }
    if(!trigger || !menu) return;
    container.classList.add('user-menu');
    trigger.classList.add('user-trigger');
    menu.classList.add('user-dropdown');
    container.setAttribute('aria-expanded','false');
    function open(){ container.setAttribute('aria-expanded','true'); }
    function close(){ container.setAttribute('aria-expanded','false'); }
    function toggle(){ container.getAttribute('aria-expanded')==='true'?close():open(); }
    trigger.addEventListener('click',(e)=>{ if(trigger.tagName==='A') e.preventDefault(); toggle(); });
    doc.addEventListener('click',(e)=>{ if(!container.contains(e.target)) close(); });
    doc.addEventListener('keydown',(e)=>{ if(e.key==='Escape') close(); });
  }
  function boot(){
    const logged = doc.querySelector('[data-auth="logged"]');
    if(!logged) return;
    const host = logged.querySelector('#authName')?.closest('[data-menu],[data-user-menu],.nav-user,.menu-user,.dropdown,.user-menu') || logged;
    setup(host);
  }
  if(doc.readyState==='loading') doc.addEventListener('DOMContentLoaded',boot); else boot();
})(window,document);
