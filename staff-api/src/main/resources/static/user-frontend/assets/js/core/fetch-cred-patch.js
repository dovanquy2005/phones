// fetch-cred-patch.js (robust version)
// - preserve caller headers (Headers / object / array)
// - only attach Authorization if not already present
// - only set credentials = 'include' when caller didn't specify
(function(){
  const _fetch = window.fetch.bind(window);

  function readTokenFromStorage() {
    try {
      return (
        localStorage.getItem('fs_token') ||
        sessionStorage.getItem('fs_token') ||
        localStorage.getItem('accessToken') ||
        sessionStorage.getItem('accessToken') ||
        localStorage.getItem('access_token') ||
        sessionStorage.getItem('access_token') ||
        null
      );
    } catch (e) {
      console.warn('[fetch-cred-patch] storage read failed', e);
      return null;
    }
  }

  // normalize token: strip "Bearer " and surrounding quotes
  function normalizeToken(t) {
    if (!t) return null;
    let s = String(t).trim();
    s = s.replace(/^Bearer\s+/i, '').trim();
    if (/^".+"$/.test(s) || /^'.+'$/.test(s)) s = s.slice(1, -1);
    return s || null;
  }

  // Helper: create a Headers instance from many input shapes
  function toHeaders(inputHeaders) {
    const h = new Headers();
    if (!inputHeaders) return h;
    if (inputHeaders instanceof Headers) {
      inputHeaders.forEach((v, k) => h.set(k, v));
      return h;
    }
    if (Array.isArray(inputHeaders)) {
      inputHeaders.forEach(([k, v]) => h.set(k, v));
      return h;
    }
    if (typeof inputHeaders === 'object') {
      Object.entries(inputHeaders).forEach(([k, v]) => {
        // skip undefined
        if (v === undefined || v === null) return;
        h.set(k, String(v));
      });
      return h;
    }
    return h;
  }

  window.fetch = function(input, init){
    // Clone init so we don't mutate caller's object
    init = init ? Object.assign({}, init) : {};

    // If caller didn't provide credentials explicitly, default to include
    if (!('credentials' in init)) {
      init.credentials = 'include';
    }

    // read & normalize token
    let token = null;
    try { token = normalizeToken(readTokenFromStorage()); } catch(e) { token = null; }

    if (token) {
      // Build headers from whichever place caller provided them
      // Priority: init.headers -> input.headers (if Request) -> undefined
      const originalHeaders = init.headers || (input instanceof Request ? input.headers : undefined);
      const headers = toHeaders(originalHeaders);

      // If Authorization not already present (case-insensitive), then set it
      if (!headers.has('Authorization')) {
        headers.set('Authorization', 'Bearer ' + token);
      }

      // Preserve other header behavior by assigning the Headers instance
      init.headers = headers;
    } else {
      // if no token, do not modify headers; leave init.headers as-is
    }

    try {
      if (input instanceof Request) {
        // Create a new Request merging the original Request and the new init
        // Important: pass the final headers instance (if set) to the Request constructor
        const merged = new Request(input, init);
        return _fetch(merged);
      }
      // otherwise call fetch with string URL and init
      return _fetch(input, init);
    } catch (e) {
      // In case constructing new Request fails for some reason, fallback to original fetch
      console.warn('[fetch-cred-patch] failed to build merged Request, falling back', e);
      return _fetch(input, init);
    }
  };

  console.log('%cfetch-cred-patch: loaded (attach Authorization if token present)', 'color:#2d98da');
})();
