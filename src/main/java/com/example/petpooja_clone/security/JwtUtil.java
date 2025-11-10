package com.example.petpooja_clone.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private final Key key;
    private final long expiration = 1000 * 60 * 60; // 1 hour

    public JwtUtil() {
        String secret = System.getenv("JWT_SECRET");
        Key tempKey;
        if (secret == null || secret.isEmpty()) {
            log.warn("JWT_SECRET not set, using in-memory key (NOT SECURE FOR PRODUCTION)");
            tempKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        } else {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(secret);
                if (keyBytes.length < 32) {
                    log.error("JWT_SECRET must be at least 256 bits (32 bytes) when base64 decoded");
                    throw new IllegalArgumentException("JWT_SECRET too short");
                }
                tempKey = Keys.hmacShaKeyFor(keyBytes);
                log.info("JWT secret loaded from environment variable");
            } catch (IllegalArgumentException e) {
                log.error("Invalid JWT_SECRET format, using in-memory key", e);
                tempKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            }
        }
        this.key = tempKey;
    }

    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public String extractRole(String token) {
        return (String) Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("role");
    }

}

