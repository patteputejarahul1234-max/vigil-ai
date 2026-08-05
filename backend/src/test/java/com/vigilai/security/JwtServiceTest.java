package com.vigilai.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtService has no Spring dependencies of its own, so this is a plain
 * unit test — no application context needed, runs in milliseconds.
 */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "test-secret-key-that-is-long-enough-for-hmac-sha-256",
                60_000,   // 1 minute access token
                600_000   // 10 minute refresh token
        );
    }

    @Test
    void generatedAccessTokenContainsCorrectEmailAndClaims() {
        String token = jwtService.generateAccessToken("user@example.com", 42L, "USER");

        assertEquals("user@example.com", jwtService.extractEmail(token));
        assertEquals(42L, jwtService.extractUserId(token));
        assertEquals("USER", jwtService.extractRole(token));
        assertFalse(jwtService.isExpired(token));
    }

    @Test
    void isTokenValidReturnsTrueForMatchingEmailAndUnexpiredToken() {
        String token = jwtService.generateAccessToken("user@example.com", 1L, "USER");
        assertTrue(jwtService.isTokenValid(token, "user@example.com"));
    }

    @Test
    void isTokenValidReturnsFalseForMismatchedEmail() {
        String token = jwtService.generateAccessToken("user@example.com", 1L, "USER");
        assertFalse(jwtService.isTokenValid(token, "someone-else@example.com"));
    }

    @Test
    void expiredTokenThrowsWhenParsed() {
        JwtService shortLived = new JwtService(
                "test-secret-key-that-is-long-enough-for-hmac-sha-256",
                1,      // 1ms access token — expires almost immediately
                600_000
        );
        String token = shortLived.generateAccessToken("user@example.com", 1L, "USER");

        try {
            Thread.sleep(20);
        } catch (InterruptedException ignored) {}

        assertThrows(ExpiredJwtException.class, () -> shortLived.extractEmail(token));
    }

    @Test
    void refreshTokenDoesNotCarryRoleClaim() {
        String refreshToken = jwtService.generateRefreshToken("user@example.com", 1L);
        // Refresh tokens intentionally omit role — role must come fresh from a real access token
        assertNull(jwtService.extractRole(refreshToken));
    }
}
