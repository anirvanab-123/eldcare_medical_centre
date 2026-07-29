package com.neuedu.eldercare.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final Duration expiration;

    public JwtUtil(
            @Value("${app.jwt-secret}") String secret,
            @Value("${app.jwt-expiration-hours}") long hours) {
        this.key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
        this.expiration = Duration.ofHours(hours);
    }

    public String create(
            Long id, String username,
            String role, String realName) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(id))
                .claim("username", username)
                .claim("role", role)
                .claim("realName", realName)
                .issuedAt(now)
                .expiration(new Date(
                        now.getTime() + expiration.toMillis()
                ))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
