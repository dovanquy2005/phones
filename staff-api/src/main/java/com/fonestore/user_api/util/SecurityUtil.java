package com.fonestore.user_api.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * SecurityUtil - reusable helper to resolve user id from SecurityContext (robust against different principal shapes)
 *
 * Behavior:
 *  - If principal is Number -> accept
 *  - If Jwt class present -> try getClaimAsString("userId"/"uid"/"id") or getSubject()
 *  - If principal implements UserDetails -> try getId() reflectively, else use auth.getName()
 *  - If principal is Map -> check common keys (userId, uid, user_id, id, sub)
 *  - If principal is String -> try parse it as number or fallback to auth.getName()
 *
 * Also exposes extractBearer(request) to read Authorization header.
 *
 * Important: keep this class small and dependency-light so it can be reused by controllers/services.
 */
@Component
public class SecurityUtil {

    /**
     * Resolve user id from either:
     *  - provided userId param (but only accepted if it matches token principal)
     *  - or, resolve from SecurityContextHolder Authentication principal (Jwt via reflection, UserDetails, Map, String, Number)
     *
     * Returns null if unable to resolve or not authenticated.
     */
    public Long resolveUserId(Long userIdParam) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();
        Long resolved = null;

        try {
            // 0) If principal is a Number (e.g. Long), accept it directly
            if (principal instanceof Number) {
                resolved = ((Number) principal).longValue();
            }

            // 1) Try reflectively detect Jwt class if present
            if (resolved == null) {
                try {
                    Class<?> jwtClass = Class.forName("org.springframework.security.oauth2.jwt.Jwt");
                    if (jwtClass.isInstance(principal)) {
                        try {
                            Method getClaimAsString = jwtClass.getMethod("getClaimAsString", String.class);
                            Object claimUserId = null;
                            try { claimUserId = getClaimAsString.invoke(principal, "userId"); } catch (Throwable ignore) {}
                            if (claimUserId == null) {
                                try { claimUserId = getClaimAsString.invoke(principal, "uid"); } catch (Throwable ignore) {}
                            }
                            if (claimUserId == null) {
                                try { claimUserId = getClaimAsString.invoke(principal, "id"); } catch (Throwable ignore) {}
                            }
                            if (claimUserId == null) {
                                // fallback to getSubject()
                                try {
                                    Method getSub = jwtClass.getMethod("getSubject");
                                    claimUserId = getSub.invoke(principal);
                                } catch (Throwable ignore) {}
                            }
                            resolved = parseLongClaim(claimUserId);
                        } catch (NoSuchMethodException nsme) {
                            // ignore
                        }
                    }
                } catch (ClassNotFoundException cnfe) {
                    // Jwt class not present - skip
                }
            }

            // 2) If still null and principal is UserDetails (or custom)
            if (resolved == null && principal != null && principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                try {
                    Method m = principal.getClass().getMethod("getId");
                    Object idVal = m.invoke(principal);
                    resolved = parseLongClaim(idVal);
                } catch (NoSuchMethodException nsme) {
                    resolved = parseLongClaim(auth.getName());
                }
            }

            // 3) principal is Map-like (claims)
            if (resolved == null && principal instanceof Map) {
                Map<?,?> map = (Map<?,?>) principal;
                Object idVal = map.get("userId");
                if (idVal == null) idVal = map.get("uid");
                if (idVal == null) idVal = map.get("user_id");
                if (idVal == null) idVal = map.get("id");
                if (idVal == null) idVal = map.get("sub");
                resolved = parseLongClaim(idVal);
            }

            // 4) principal is String
            if (resolved == null && principal instanceof String) {
                resolved = parseLongClaim((String) principal);
                if (resolved == null) resolved = parseLongClaim(auth.getName());
            }

        } catch (Exception ignored) {
            // swallow and proceed to final checks
        }

        // If client provided userId param, require it to match resolved id.
        if (userIdParam != null) {
            if (resolved == null) return null;
            if (!userIdParam.equals(resolved)) return null;
            return resolved;
        }

        return resolved;
    }

    // utility to convert claim into Long (handles Number, numeric String, etc.)
    public Long parseLongClaim(Object claim) {
        if (claim == null) return null;
        if (claim instanceof Number) {
            return ((Number) claim).longValue();
        }
        try {
            String s = String.valueOf(claim).trim();
            if (s.isEmpty()) return null;
            return Long.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    public String extractBearer(HttpServletRequest request) {
        if (request == null) return null;
        String h = request.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) return h.substring(7);
        return null;
    }
}
