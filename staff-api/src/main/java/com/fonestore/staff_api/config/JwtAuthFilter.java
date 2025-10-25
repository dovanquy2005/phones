package com.fonestore.staff_api.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String path = req.getServletPath();

        // 1) BỎ QUA preflight CORS
        if (HttpMethod.OPTIONS.matches(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        // 2) BỎ QUA static & public (không cần parse JWT)
        if (path.startsWith("/api/public/")
                || path.startsWith("/assets/")
                || path.startsWith("/user-frontend/")
                || path.startsWith("/staff-frontend/")
                || path.equals("/") || path.equals("/index.html")
                || path.equals("/favicon.ico")) {
            chain.doFilter(req, res);
            return;
        }

        // 3) THỬ đọc JWT nếu có
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Claims claims = jwtUtil.validateAndGetClaims(token);
                String principal = claims.getSubject();

                // HỖ TRỢ CẢ "role": "staff" và "roles": ["staff","manager"]
                List<String> roles = new ArrayList<>();
                String role = claims.get("role", String.class);
                if (role != null && !role.isBlank()) {
                    roles.add(role.trim());
                } else {
                    @SuppressWarnings("unchecked")
                    List<String> arr = claims.get("roles", List.class);
                    if (arr != null) {
                        for (Object x : arr) {
                            if (x != null) roles.add(Objects.toString(x).trim());
                        }
                    }
                }
                if (roles.isEmpty()) {
                    roles.add("user"); // mặc định tối thiểu
                }

                if (principal != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var authorities = roles.stream()
                            .filter(s -> s != null && !s.isBlank())
                            .map(SimpleGrantedAuthority::new) // <-- authority thuần: "staff"/"manager"
                            .toList();

                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, authorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
                // token lỗi/hết hạn -> để Security xử lý tiếp (401/403 nếu cần)
            }
        }

        chain.doFilter(req, res);
    }
}
