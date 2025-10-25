package com.fonestore.staff_api.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationSeconds;
    private final String issuer;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds,
            @Value("${app.jwt.issuer:fonestore}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // >= 32 ký tự
        this.expirationSeconds = expirationSeconds;
        this.issuer = issuer;
    }

    public String generateToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        JwtBuilder b = Jwts.builder()
                .setSubject(subject)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key, SignatureAlgorithm.HS256);
        if (claims != null && !claims.isEmpty()) b.addClaims(claims);
        return b.compact();
    }

    public Claims validateAndGetClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)   // jjwt 0.11.x
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
