// staff-frontend/assets/js/api.js
const API_BASE = window.location.origin; // same-origin (http://localhost:9090)

function getToken() { return localStorage.getItem("accessToken"); }
function setToken(t) { if (t) localStorage.setItem("accessToken", t); }
function clearToken() { localStorage.removeItem("accessToken"); }

// fetch có kèm Authorization
async function authFetch(path, options = {}) {
  const url = path.startsWith("/") ? path : "/" + path;
  const headers = new Headers(options.headers || {});
  const t = getToken();
  if (t) headers.set("Authorization", `Bearer ${t}`);
  // tự đặt Content-Type nếu body là JSON (không áp cho FormData)
  if (!headers.has("Content-Type") && options.body && !(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  let res = await fetch(`${API_BASE}${url}`, { ...options, headers });

  // retry nếu 401 bằng refresh (nếu BE của bạn có /api/auth/refresh)
  if (res.status === 401) {
    const r = await fetch("/api/auth/refresh", { method: "POST", credentials: "include" });
    if (r.ok) {
      const { accessToken } = await r.json();
      setToken(accessToken);
      const headers2 = new Headers(headers);
      headers2.set("Authorization", `Bearer ${accessToken}`);
      res = await fetch(`${API_BASE}${url}`, { ...options, headers: headers2 });
    }
  }
  return res;
}

// tiện export
window.API = { authFetch, getToken, setToken, clearToken };
