package com.smartcart.security;

import com.smartcart.entity.Role;
import com.smartcart.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";
    private final JwtService jwtService = new JwtService(SECRET, 60_000);

    private CustomUserDetails admin() {
        return new CustomUserDetails(User.builder().id(7L).email("a@b.com").password("hash").role(Role.ADMIN).build());
    }

    @Test
    void generatedTokenContainsSubjectAndRole() {
        Claims claims = jwtService.parse(jwtService.generateToken(admin()));
        assertEquals("a@b.com", claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService other = new JwtService("another-secret-another-secret-12345678", 60_000);
        String forged = other.generateToken(admin());
        assertThrows(JwtException.class, () -> jwtService.parse(forged));
    }

    @Test
    void expiredTokenIsRejected() {
        String expired = new JwtService(SECRET, -1_000).generateToken(admin());
        assertThrows(JwtException.class, () -> jwtService.parse(expired));
    }

    @Test
    void shortSecretIsRefusedAtStartup() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 1000));
    }
}
