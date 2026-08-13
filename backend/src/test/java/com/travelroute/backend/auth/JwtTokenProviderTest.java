package com.travelroute.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-at-least-32-characters-long!!";

    @Test
    void generateToken_thenParseUserId_returnsSameUserId() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, 3600));

        String token = provider.generateToken(42L);

        assertThat(provider.parseUserId(token)).isEqualTo(42L);
    }

    @Test
    void parseUserId_throwsExpiredJwtException_forAlreadyExpiredToken() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, -10));

        String token = provider.generateToken(1L);

        assertThatThrownBy(() -> provider.parseUserId(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parseUserId_throwsJwtException_forGarbageToken() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, 3600));

        assertThatThrownBy(() -> provider.parseUserId("not-a-real-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseUserId_throwsJwtException_whenSignedWithDifferentSecret() {
        JwtTokenProvider issuer = new JwtTokenProvider(new JwtProperties(SECRET, 3600));
        JwtTokenProvider verifier = new JwtTokenProvider(
                new JwtProperties("a-completely-different-secret-key-value!!", 3600));

        String token = issuer.generateToken(1L);

        assertThatThrownBy(() -> verifier.parseUserId(token))
                .isInstanceOf(JwtException.class);
    }
}
