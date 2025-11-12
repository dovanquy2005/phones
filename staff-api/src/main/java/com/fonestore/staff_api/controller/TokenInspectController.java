package com.fonestore.staff_api.controller;

import com.fonestore.staff_api.config.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TokenInspectController {
    private final JwtUtil jwtUtil;
    public TokenInspectController(JwtUtil jwtUtil) { this.jwtUtil = jwtUtil; }

    @GetMapping("/api/public/inspect-token")
    public Map<String, Object> inspect(@RequestHeader(value="Authorization", required=false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return Map.of("ok", false, "error", "no_authorization_header", "received", auth);
        }
        String token = auth.substring(7);
        try {
            Claims claims = jwtUtil.validateAndGetClaims(token);
            
            return Map.of("ok", true, "claims", claims);
        } catch (Exception ex) {
            return Map.of("ok", false, "error", ex.getClass().getSimpleName(), "message", ex.getMessage());
        }
    }
}