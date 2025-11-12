package com.fonestore.user_api.config; // <-- chỉnh lại package cho đúng module user-api

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fonestore.staff_api.config.JwtUtil;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JwtAuthFilter cho user-api
 * - In debug (headers, raw Authorization)
 * - Validate token via JwtUtil.validateAndGetClaims(token)
 * - Map claims -> authorities (normalize ROLE_ prefix, lower-case)
 * - Set SecurityContextHolder nếu valid
 *
 * Paste vào module user-api (chỉnh package), build + restart.
 */
@Component
@Order(100) // chạy sớm; nếu cần chạy sớm hơn, tăng priority (small number = high precedence)
public class UserJwtAuthFilter extends OncePerRequestFilter {

    private final Logger log = LoggerFactory.getLogger(UserJwtAuthFilter.class);
    private final JwtUtil jwtUtil;

    public UserJwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {
                // Debug: show request start and headers (temporary)
            System.out.println("---- request headers ----");
            Collections.list(req.getHeaderNames()).forEach(h -> System.out.println(h + ": " + req.getHeader(h)));
            System.out.println("-------------------------");
        final String path = req.getServletPath();
        
        // Debug: show request start and headers (temporary)
        log.debug(">>>> JwtAuthFilter START path={} method={}", path, req.getMethod());
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

        // skip preflight and static/public
        if (HttpMethod.OPTIONS.matches(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }
        if (path.startsWith("/api/public/") ||
            path.startsWith("/assets/") ||
            path.startsWith("/user-frontend/") ||
            path.startsWith("/staff-frontend/") ||
            path.equals("/") || path.equals("/index.html") || path.equals("/favicon.ico")) {
            chain.doFilter(req, res);
            return;
        }

        // Obtain Authorization header or dev fallback (query param token)
        String auth = req.getHeader("Authorization");
        if ((auth == null || auth.isBlank()) && req.getParameter("authorization") != null) {
            auth = req.getParameter("authorization");
        }
        if ((auth == null || auth.isBlank()) && req.getParameter("token") != null) {
            String t = req.getParameter("token");
            auth = t.toLowerCase().startsWith("bearer ") ? t : "Bearer " + t;
        }
        log.debug(">>>> JwtAuthFilter raw Authorization header: {}", auth);

        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            log.debug(">>>> JwtAuthFilter: token (len={}): {}", token.length(),
                    token.length() > 40 ? token.substring(0, 40) + "..." : token);
            try {
                Claims claims = jwtUtil.validateAndGetClaims(token);
                if (claims == null) {
                    log.debug(">>>> JwtAuthFilter: jwtUtil returned null claims (invalid token?)");
                    chain.doFilter(req, res);
                    return;
                }

                // log some common claims for debugging
                log.debug(">>>> JwtAuthFilter: claims.sub={}, keys={}", claims.getSubject(), claims.keySet());
                log.debug(">>>> claims.sub={}, id={}, userId={}, role={}, roles={}",
                        claims.getSubject(),
                        claims.get("id"),
                        claims.get("userId"),
                        claims.get("role"),
                        claims.get("roles"));

                // Resolve numeric userId if present
                Long userId = null;
                Object idClaim = null;
                List<String> tryKeys = Arrays.asList("id", "userId", "user_id", "uid");
                for (String k : tryKeys) {
                    idClaim = claims.get(k);
                    if (idClaim != null) break;
                }
                if (idClaim != null) {
                    try {
                        if (idClaim instanceof Number) {
                            userId = ((Number) idClaim).longValue();
                        } else {
                            userId = Long.parseLong(Objects.toString(idClaim, ""));
                        }
                    } catch (NumberFormatException e) {
                        log.debug("JwtAuthFilter: claim id not numeric: {}", idClaim);
                        userId = null;
                    }
                }
                // fallback: subject numeric?
                String subject = claims.getSubject();
                if (userId == null && subject != null) {
                    try { userId = Long.valueOf(subject); } catch (NumberFormatException ignored) {}
                }

                // Normalize roles from claims: roles (list/string) or role (single)
                List<String> roles = new ArrayList<>();
                Object rObj = claims.get("roles");
                if (rObj == null) {
                    String single = claims.get("role", String.class);
                    if (single != null && !single.isBlank()) roles.add(single);
                } else if (rObj instanceof String) {
                    String s = ((String) rObj).trim();
                    if (!s.isEmpty()) {
                        if (s.contains(",")) for (String part : s.split(",")) roles.add(part.trim());
                        else roles.add(s);
                    }
                } else if (rObj instanceof Collection) {
                    for (Object x : (Collection<?>) rObj) if (x != null) roles.add(Objects.toString(x).trim());
                } else {
                    String single = claims.get("role", String.class);
                    if (single != null && !single.isBlank()) roles.add(single);
                }
                if (roles.isEmpty()) roles.add("user"); // default

                // Map to SimpleGrantedAuthority — strip ROLE_ prefix and lower-case
                var authorities = roles.stream()
                        .filter(r -> r != null && !r.isBlank())
                        .map(r -> {
                            String norm = r.trim();
                            if (norm.startsWith("ROLE_")) norm = norm.substring(5);
                            return new SimpleGrantedAuthority(norm.toLowerCase());
                        })
                        .collect(Collectors.toList());

                // Build principal
                Object principal = (userId != null) ? userId : (subject != null ? subject : "anonymous");

                // Add claims into details map
                Map<String,Object> details = new HashMap<>();
                for (Map.Entry<String, Object> e : claims.entrySet()) details.put(e.getKey(), e.getValue());
                if (subject != null) details.put("sub", subject);
                if (userId != null) details.put("userId", userId);

                // Set authentication
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authentication.setDetails(details);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JwtAuthFilter: set Authentication principal={} authorities={}", principal, authorities);

            } catch (ExpiredJwtException e) {
                log.warn(">>>> JwtAuthFilter: token expired: {}", e.getMessage());
            } catch (JwtException e) {
                // covers signature / malformed / other jwt errors
                log.warn(">>>> JwtAuthFilter: jwt invalid: {}", e.getMessage());
            } catch (Exception ex) {
                log.debug("JwtAuthFilter: token validation failed: {}", ex.toString());
            }
        } else {
            log.debug("JwtAuthFilter: no Bearer token present for path {}", path);
        }

        chain.doFilter(req, res);
    }
}
