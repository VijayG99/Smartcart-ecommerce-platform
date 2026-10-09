package com.smartcart.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Creates and validates signed JWTs (HS256). Tokens are never logged. */
@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${smartcart.jwt.secret}") String secret,
                      @Value("${smartcart.jwt.expiration-ms}") long expirationMs) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(CustomUserDetails user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Verifies signature and expiry; throws a JwtException subtype when the token is invalid. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
