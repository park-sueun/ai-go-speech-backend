package com.aigo.speech.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-characters-long!!";
    private static final String EMAIL = "user@example.com";

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, 30, 7);
    }

    @Test
    @DisplayName("액세스 토큰 생성 시 null이 아닌 토큰을 반환한다")
    void createAccessToken_returnsNonNullToken() {
        String token = jwtTokenProvider.createAccessToken(EMAIL);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("리프레시 토큰 생성 시 null이 아닌 토큰을 반환한다")
    void createRefreshToken_returnsNonNullToken() {
        String token = jwtTokenProvider.createRefreshToken(EMAIL);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("유효한 토큰 검증 시 true를 반환한다")
    void validateToken_withValidToken_returnsTrue() {
        String token = jwtTokenProvider.createAccessToken(EMAIL);
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("잘못된 형식의 토큰 검증 시 false를 반환한다")
    void validateToken_withInvalidToken_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("not.a.valid.token")).isFalse();
    }

    @Test
    @DisplayName("토큰에서 이메일을 올바르게 추출한다")
    void getEmail_extractsCorrectEmail() {
        String token = jwtTokenProvider.createAccessToken(EMAIL);
        assertThat(jwtTokenProvider.getEmail(token)).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("액세스 토큰과 리프레시 토큰은 서로 다르다")
    void accessAndRefreshTokens_areDifferent() {
        String access = jwtTokenProvider.createAccessToken(EMAIL);
        String refresh = jwtTokenProvider.createRefreshToken(EMAIL);
        assertThat(access).isNotEqualTo(refresh);
    }
}
