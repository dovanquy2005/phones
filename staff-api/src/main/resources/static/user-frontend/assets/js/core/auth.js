// assets/js/core/auth.js
// Canonical auth helper for frontend (improved).
// Exports: getToken, setToken, clearAuth, getCurrentUserId
// Also exposes globals for legacy scripts: window.getToken, window.setToken, ...
// Handles fs_token / accessToken / access_token across localStorage + sessionStorage
// Emits fs:auth:ready on init and fs:auth:login / fs:auth:logout on changes.

const API = (window.FS_API && String(window.FS_API)) || 'http://localhost:9090';
if (!window.FS_API) {
  try { window.FS_API = API; } catch (e) { /* ignore */ }
}

function safeGetStorage(key) {
  try {
    const v1 = localStorage.getItem(key);
    if (v1 != null) return v1;
    const v2 = sessionStorage.getItem(key);
    return v2 != null ? v2 : null;
  } catch (e) {
    return null;
  }
}

function safeSetStorageBoth(key, value) {
  try { localStorage.setItem(key, value); } catch (e) { /* ignore */ }
  try { sessionStorage.setItem(key, value); } catch (e) { /* ignore */ }
}

function safeRemoveStorageBoth(key) {
  try { localStorage.removeItem(key); } catch (e) { /* ignore */ }
  try { sessionStorage.removeItem(key); } catch (e) { /* ignore */ }
}

/** Normalize token string: strip "Bearer " prefix and surrounding quotes and whitespace. */
function normalizeTokenInput(token) {
  if (!token) return '';
  let t = String(token).trim();
  if (/^".+"$/.test(t)) t = t.slice(1, -1);
  if (/^'.+'$/.test(t)) t = t.slice(1, -1);
  if (/^Bearer\s+/i.test(t)) t = t.replace(/^Bearer\s+/i, '').trim();
  return t;
}

/** Return token string (without "Bearer " prefix) or empty string. Checks multiple keys. */
export function getToken() {
  const candidate = safeGetStorage('fs_token') || safeGetStorage('accessToken') || safeGetStorage('access_token') || '';
  return normalizeTokenInput(candidate);
}

/**
 * Helper: classify a claim value as 'id' or 'email' or 'unknown'
 */
function classifyUserClaim(v) {
  if (v == null) return { type: 'unknown', value: null };
  const s = String(v);
  // looks like email?
  const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (emailRe.test(s)) return { type: 'email', value: s };
  // numeric id?
  if (!Number.isNaN(Number(s))) return { type: 'id', value: s };
  // uuid-like (hex + dashes)
  if (/^[0-9a-fA-F]{8}-[0-9a-fA-F-]{24,}$/i.test(s) || /^[0-9a-fA-F-]{8,36}$/.test(s)) {
    return { type: 'id', value: s };
  }
  return { type: 'unknown', value: s };
}

/**
 * Safely decode JWT payload and return candidate for user id/email.
 * Returns object: { type: 'id'|'email'|'unknown', value } or null on parse failure.
 */
function extractUserClaimFromJWT(token) {
  if (!token || typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length < 2) return null;
  try {
    let payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const pad = payload.length % 4;
    if (pad === 2) payload += '==';
    else if (pad === 3) payload += '=';
    // If pad ===1 then ignore (invalid)
    if (!/^[A-Za-z0-9+/=]+$/.test(payload)) return null;
    const obj = JSON.parse(atob(payload));
    if (obj == null || typeof obj !== 'object') return null;
    const raw = obj.userId ?? obj.id ?? obj.uid ?? obj.sub ?? null;
    return raw == null ? { type: 'unknown', value: null } : classifyUserClaim(raw);
  } catch (e) {
    return null;
  }
}

/**
 * Persist token (stores canonical + compatibility keys) into both storages.
 * Accepts token either as raw JWT or with "Bearer " prefix.
 * Returns userId (string/number) if available and classified as id, otherwise null.
 * If claim is email, saves to fs_user_email (NOT fs_user_id).
 */
export function setToken(token) {
  const raw = normalizeTokenInput(token);
  const storeVal = raw ? String(raw) : '';
  try {
    // write token keys to both storages for compatibility
    safeSetStorageBoth('fs_token', storeVal);
    safeSetStorageBoth('accessToken', storeVal);
    safeSetStorageBoth('access_token', storeVal);
  } catch (e) { /* ignore */ }

  // inspect claim
  const info = extractUserClaimFromJWT(storeVal);
  let uid = null;
  try {
    if (info && info.type === 'id' && info.value != null) {
      // store as user id
      safeSetStorageBoth('fs_user_id', String(info.value));
      // remove any leftover email field
      safeRemoveStorageBoth('fs_user_email');
      uid = info.value;
    } else if (info && info.type === 'email' && info.value != null) {
      // store email separately; do NOT set fs_user_id
      safeSetStorageBoth('fs_user_email', String(info.value));
      safeRemoveStorageBoth('fs_user_id');
      uid = null;
    } else {
      // unknown: cleanup both fields
      safeRemoveStorageBoth('fs_user_id');
      safeRemoveStorageBoth('fs_user_email');
      uid = null;
    }
  } catch (e) { /* ignore */ }

  try {
    window.dispatchEvent(new CustomEvent('fs:auth:login', { detail: { userId: uid ?? null } }));
  } catch (e) { /* ignore */ }

  return uid ?? null;
}

/**
 * Remove auth keys from both storages and dispatch logout event.
 */
export function clearAuth() {
  try {
    ['fs_token','accessToken','access_token','fs_user_id','fs_user_email'].forEach(k => safeRemoveStorageBoth(k));
  } catch (e) { /* ignore */ }

  try {
    window.dispatchEvent(new CustomEvent('fs:auth:logout'));
  } catch (e) { /* ignore */ }
}

/**
 * Get current user id (Number or string) or null.
 * Only returns value if it's a valid id (numeric or uuid-like).
 */
export function getCurrentUserId() {
  try {
    const s = safeGetStorage('fs_user_id');
    if (s != null && s !== '') {
      // numeric?
      if (!Number.isNaN(Number(s))) return Number(s);
      return s;
    }
  } catch (e) { /* ignore */ }

  const token = getToken();
  const info = extractUserClaimFromJWT(token);
  if (info && info.type === 'id' && info.value != null) {
    try { safeSetStorageBoth('fs_user_id', String(info.value)); } catch (e) {}
    const n = Number(info.value);
    return Number.isNaN(n) ? info.value : n;
  }
  return null;
}

// Expose legacy globals for non-module code
try {
  window.getToken = getToken;
  window.setToken = setToken;
  window.clearAuth = clearAuth;
  window.getCurrentUserId = getCurrentUserId;
  if (!window.FS_API) window.FS_API = API;
} catch (e) { /* ignore */ }

/**
 * Emit fs:auth:ready so scripts that loaded before the module can react.
 * Payload includes token (normalized string) and userId (if known).
 * Re-emit shortly after for safety.
 */
(function emitAuthReady(){
  try {
    const payload = { token: getToken(), userId: getCurrentUserId() };
    window.dispatchEvent(new CustomEvent('fs:auth:ready', { detail: payload }));
    setTimeout(() => {
      try { window.dispatchEvent(new CustomEvent('fs:auth:ready', { detail: payload })); } catch(e){}
    }, 50);
  } catch (e) { /* ignore */ }
})();
