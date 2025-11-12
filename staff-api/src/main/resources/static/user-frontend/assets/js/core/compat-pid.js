;(function(doc,win){
  function rewrite(){
    doc.querySelectorAll('a[href*="product-detail.html?pid="]').forEach(a=>{
      a.href = a.href.replace('product-detail.html?pid=','product-detail.html?id=');
    });
  }
  function fixSelf(){
    if(!/product-detail\.html$/.test(location.pathname)) return;
    const u = new URL(location.href);
    if(u.searchParams.has('id')) return;
    if(u.searchParams.has('pid')){
      const pid = u.searchParams.get('pid');
      u.searchParams.delete('pid');
      u.searchParams.set('id', pid);
      location.replace(u.toString());
    }
  }
  if(doc.readyState==='loading') doc.addEventListener('DOMContentLoaded',rewrite); else rewrite();
  fixSelf();
})(document,window);
