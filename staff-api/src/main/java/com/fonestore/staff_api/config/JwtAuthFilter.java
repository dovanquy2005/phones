package com.fonestore.staff_api.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Improved JwtAuthFilter (normalized roles, safe claims -> Map, better logging)
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String path = req.getServletPath();

        // --- DEBUG BLOCK: in ra header + token + claims để debug why 401 ---
        log.debug(">>>> JwtAuthFilter START path={} method={}", req.getServletPath(), req.getMethod());

        // print all incoming headers (helpful khi header bị strip by proxy)
        try {
            var names = req.getHeaderNames();
            if (names != null) {
                names.asIterator().forEachRemaining(h -> log.debug(">>>> hdr: {} = {}", h, req.getHeader(h)));
            } else {
                log.debug(">>>> hdr: headerNames is null");
            }
        } catch (Exception ex) {
            log.debug(">>>> hdr: error iterating headers: {}", ex.toString());
        }
        // --- end debug header block ---

        // skip OPTIONS preflight
        if (HttpMethod.OPTIONS.matches(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        // skip some public/static paths quickly
        if (path.startsWith("/api/public/")
                || path.startsWith("/assets/")
                || path.startsWith("/user-frontend/")
                || path.startsWith("/staff-frontend/")
                || path.equals("/") || path.equals("/index.html")
                || path.equals("/favicon.ico")) {
            chain.doFilter(req, res);
            return;
        }

        // get Authorization header and normalize (with your existing dev fallback)
        String auth = req.getHeader("Authorization");
        // fallback: check query param 'authorization' or 'token' (dev only)
        if ((auth == null || auth.isBlank()) && req.getParameter("authorization") != null) {
            auth = req.getParameter("authorization");
        }
        if ((auth == null || auth.isBlank()) && req.getParameter("token") != null) {
            String t = req.getParameter("token");
            auth = t.toLowerCase().startsWith("bearer ") ? t : "Bearer " + t;
        }

        // debug raw Authorization header
        log.debug(">>>> JwtAuthFilter raw Authorization header: {}", auth);

        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            log.debug(">>>> JwtAuthFilter: token (len={}): {}", token.length(),
                    token.length() > 30 ? token.substring(0, 30) + "..." : token);
            try {
                Claims claims = jwtUtil.validateAndGetClaims(token);
                if (claims == null) {
                    log.debug(">>>> JwtAuthFilter: jwtUtil returned null claims (token invalid?)");
                    chain.doFilter(req, res);
                    return;
                } else {
                    log.debug(">>>> JwtAuthFilter: claims found: sub={}, keys={}", claims.getSubject(), claims.keySet());
                    log.debug(">>>> claims.sub={}, id={}, userId={}, role={}, roles={}",
                            claims.getSubject(),
                            claims.get("id"),
                            claims.get("userId"),
                            claims.get("role"),
                            claims.get("roles")
                    );
                }

                // try common id keys
                Long userId = null;
                Object idClaim = null;
                List<String> tryKeys = Arrays.asList("id", "userId", "user_id","uid");
                for (String k : tryKeys) {
                    idClaim = claims.get(k);
                    if (idClaim != null) break;
                }

                if (idClaim != null) {
                    try {
                        if (idClaim instanceof Number) {
                            userId = ((Number) idClaim).longValue();
                        } else {
                            String s = Objects.toString(idClaim, "");
                            userId = Long.parseLong(s);
                        }
                    } catch (NumberFormatException ex) {
                        log.debug("JwtAuthFilter: claim id not numeric: {}", idClaim);
                        userId = null;
                    }
                }

                // fallback: subject numeric?
                String subject = claims.getSubject();
                if (userId == null && subject != null) {
                    try {
                        userId = Long.valueOf(subject);
                    } catch (NumberFormatException ignored) {}
                }

                // Roles: normalize possible shapes
                List<String> roles = new ArrayList<>();
                Object rObj = claims.get("roles");
                if (rObj == null) {
                    String single = claims.get("role", String.class);
                    if (single != null && !single.isBlank()) roles.add(single);
                } else if (rObj instanceof String) {
                    // maybe comma separated
                    String s = ((String) rObj).trim();
                    if (!s.isEmpty()) {
                        if (s.contains(",")) {
                            for (String part : s.split(",")) roles.add(part.trim());
                        } else {
                            roles.add(s);
                        }
                    }
                } else if (rObj instanceof Collection) {
                    for (Object x : (Collection<?>) rObj) {
                        if (x != null) roles.add(Objects.toString(x).trim());
                    }
                } else {
                    // unknown shape, fallback to role claim
                    String single = claims.get("role", String.class);
                    if (single != null && !single.isBlank()) roles.add(single);
                }

                if (roles.isEmpty()) {
                    roles.add("user"); // default if nothing provided
                }

                // normalize roles to match hasAuthority("user") in config:
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .filter(r -> r != null && !r.isBlank())
                        .map(r -> {
                            String norm = r.trim();
                            if (norm.startsWith("ROLE_")) norm = norm.substring(5);
                            return new SimpleGrantedAuthority(norm.toLowerCase());
                        })
                        .collect(Collectors.toList());

                // principal: prefer numeric id if available else subject string
                Object principalObj = userId != null ? userId : (subject != null ? subject : "anonymous");

                // copy claims into a safe Map<String,Object>
                Map<String, Object> details = new HashMap<>();
                for (Map.Entry<String, Object> e : claims.entrySet()) {
                    details.put(e.getKey(), e.getValue());
                }
                if (subject != null) details.put("sub", subject);
                if (userId != null) details.put("userId", userId);

                var authentication = new UsernamePasswordAuthenticationToken(principalObj, null, authorities);
                authentication.setDetails(details);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JwtAuthFilter: set Authentication principal={} authorities={}", principalObj, authorities);

            } 
            catch (ExpiredJwtException e) {
                log.warn(">>>> JwtAuthFilter: token expired: {}", e.getMessage());
            }
            catch (JwtException e) {   // covers SignatureException, MalformedJwtException, etc
                log.warn(">>>> JwtAuthFilter: jwt invalid: {}", e.getMessage());
            }
            catch (Exception ex) {
                log.debug("JwtAuthFilter: token validation failed: {}", ex.toString());
            }
        } else {
            // no Authorization header
            log.debug("JwtAuthFilter: no Authorization header for path {}", path);
        }

        chain.doFilter(req, res);
    }

}
