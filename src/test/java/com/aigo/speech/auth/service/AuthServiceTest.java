package com.aigo.speech.auth.service;

import com.aigo.speech.auth.dto.AuthDto.LoginRequest;
import com.aigo.speech.auth.dto.AuthDto.SignupRequest;
import com.aigo.speech.auth.dto.AuthDto.TokenResponse;
import com.aigo.speech.auth.dto.TokenRequest;
import com.aigo.speech.auth.jwt.JwtTokenProvider;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @InjectMocks
    private AuthService authService;

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encoded_password";

    // ======================== signup ========================

    @Test
    @DisplayName("중복 이메일로 가입 시 예외가 발생한다")
    void signup_withDuplicateEmail_throwsException() {
        SignupRequest request = new SignupRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setUsername("tester");
        given(userRepository.existsByEmail(EMAIL)).willReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이미 존재하는 이메일입니다.");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("유효한 정보로 회원가입 시 사용자가 저장된다")
    void signup_withValidRequest_savesUser() {
        SignupRequest request = new SignupRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setUsername("tester");
        given(userRepository.existsByEmail(EMAIL)).willReturn(false);
        given(bCryptPasswordEncoder.encode(PASSWORD)).willReturn(ENCODED_PASSWORD);

        authService.signup(request);

        verify(userRepository).save(any(User.class));
    }

    // ======================== login ========================

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시 예외가 발생한다")
    void login_withUnknownEmail_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

    @Test
    @DisplayName("비밀번호가 틀린 경우 예외가 발생한다")
    void login_withWrongPassword_throwsException() {
        User user = User.builder().email(EMAIL).password(ENCODED_PASSWORD).username("tester").build();
        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword("wrongPassword");
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(bCryptPasswordEncoder.matches("wrongPassword", ENCODED_PASSWORD)).willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("올바른 정보로 로그인 시 액세스/리프레시 토큰을 반환한다")
    void login_withValidCredentials_returnsTokens() {
        User user = User.builder().email(EMAIL).password(ENCODED_PASSWORD).username("tester").build();
        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(bCryptPasswordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).willReturn(true);
        given(jwtTokenProvider.createAccessToken(EMAIL)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(EMAIL)).willReturn("refresh-token");

        TokenResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(user.getRefreshToken()).isEqualTo("refresh-token");
    }

    // ======================== updateRefreshToken ========================

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 갱신 시 예외가 발생한다")
    void updateRefreshToken_withInvalidToken_throwsException() {
        TokenRequest request = new TokenRequest("invalid-token");
        given(jwtTokenProvider.validateToken("invalid-token")).willReturn(false);

        assertThatThrownBy(() -> authService.updateRefreshToken(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("리프레시 토큰이 유효하지 않습니다.");
    }

    @Test
    @DisplayName("저장된 토큰과 일치하지 않는 경우 예외가 발생한다")
    void updateRefreshToken_withMismatchedToken_throwsException() {
        User user = User.builder().email(EMAIL).password(ENCODED_PASSWORD).username("tester").build();
        user.updateRefreshToken("stored-token");
        TokenRequest request = new TokenRequest("different-token");
        given(jwtTokenProvider.validateToken("different-token")).willReturn(true);
        given(jwtTokenProvider.getEmail("different-token")).willReturn(EMAIL);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.updateRefreshToken(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("일치하는 토큰 정보가 없습니다. 다시 시도해주세요.");
    }

    @Test
    @DisplayName("유효한 리프레시 토큰으로 새 토큰 쌍을 반환한다")
    void updateRefreshToken_withValidToken_returnsNewTokens() {
        User user = User.builder().email(EMAIL).password(ENCODED_PASSWORD).username("tester").build();
        user.updateRefreshToken("refresh-token");
        TokenRequest request = new TokenRequest("refresh-token");
        given(jwtTokenProvider.validateToken("refresh-token")).willReturn(true);
        given(jwtTokenProvider.getEmail("refresh-token")).willReturn(EMAIL);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(EMAIL)).willReturn("new-access-token");
        given(jwtTokenProvider.createRefreshToken(EMAIL)).willReturn("new-refresh-token");

        TokenResponse response = authService.updateRefreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(user.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    // ======================== logout ========================

    @Test
    @DisplayName("로그아웃 시 사용자의 리프레시 토큰이 초기화된다")
    void logout_clearsRefreshToken() {
        User user = User.builder().email(EMAIL).password(ENCODED_PASSWORD).username("tester").build();
        user.updateRefreshToken("refresh-token");
        given(jwtTokenProvider.getEmail("access-token")).willReturn(EMAIL);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));

        authService.logout("access-token");

        assertThat(user.getRefreshToken()).isNull();
    }
}
