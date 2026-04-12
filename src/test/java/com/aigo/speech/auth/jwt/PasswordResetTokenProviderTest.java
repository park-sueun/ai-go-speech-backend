package com.aigo.speech.auth.jwt;

import com.aigo.speech.auth.exception.InvalidTokenException;
import com.aigo.speech.auth.exception.TokenExpiredException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetTokenProviderTest {

    private static final String SECRET = "test-reset-secret-key-must-be-at-least-32-chars!!";
    private static final String EMAIL = "user@example.com";

    private PasswordResetTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new PasswordResetTokenProvider();
        ReflectionTestUtils.setField(provider, "resetSecret", SECRET);
        ReflectionTestUtils.setField(provider, "resetExpirationMin", 30L);
    }

    @Test
    @DisplayName("토큰 생성 시 null이 아닌 토큰을 반환한다")
    void generateToken_returnsNonNullToken() {
        String token = provider.generateToken(EMAIL);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("유효한 토큰에서 이메일을 올바르게 추출한다")
    void validateAndGetEmail_withValidToken_returnsEmail() {
        String token = provider.generateToken(EMAIL);
        assertThat(provider.validateAndGetEmail(token)).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("만료된 토큰 검증 시 TokenExpiredException이 발생한다")
    void validateAndGetEmail_withExpiredToken_throwsTokenExpiredException() {
        String expiredToken = Jwts.builder()
                .setSubject(EMAIL)
                .claim("type", "password-reset")
                .setIssuedAt(new Date(System.currentTimeMillis() - 10_000))
                .setExpiration(new Date(System.currentTimeMillis() - 5_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertThatThrownBy(() -> provider.validateAndGetEmail(expiredToken))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessage("만료된 토큰입니다.");
    }

    @Test
    @DisplayName("잘못된 형식의 토큰 검증 시 InvalidTokenException이 발생한다")
    void validateAndGetEmail_withMalformedToken_throwsInvalidTokenException() {
        assertThatThrownBy(() -> provider.validateAndGetEmail("not.a.valid.token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("유효하지 않은 토큰입니다.");
    }

    @Test
    @DisplayName("type claim이 다른 토큰 검증 시 InvalidTokenException이 발생한다")
    void validateAndGetEmail_withWrongType_throwsInvalidTokenException() {
        String wrongTypeToken = Jwts.builder()
                .setSubject(EMAIL)
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertThatThrownBy(() -> provider.validateAndGetEmail(wrongTypeToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("유효하지 않은 토큰 타입입니다.");
    }
}
