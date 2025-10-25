
export function readLocalUser(){
  try{
    const u = JSON.parse(localStorage.getItem('fs_user') || sessionStorage.getItem('fs_user') || 'null');
    if (u) return u;
  }catch{}
  const t = localStorage.getItem('fs_token') || sessionStorage.getItem('fs_token');
  if (t && t.split('.').length === 3){
    try{
      const p = JSON.parse(atob(t.split('.')[1]));
      return { email: p.email||p.sub, name: p.name||p.fullName||p.email||'User', userId: p.userId||p.uid||null, role: p.role||'user' };
    }catch{}
  }
  return null;
}

export async function getCurrentUser(){
  try{
    const r = await fetch('/api/auth/me', { headers: { 'Authorization': 'Bearer ' + token, 'Accept':'application/json' }, method: 'GET' });
    if(!r.ok) return null;
    return await r.json();
  }catch(e){ console.error('getCurrentUser error', e); return null; }
}
export async function doLogout(){
  try{
    await fetch('/api/auth/logout', { method: 'POST' });
  }catch(e){}
  location.href = 'login.html';
}