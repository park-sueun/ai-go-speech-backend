package com.aigo.speech.auth.service;

import com.aigo.speech.auth.exception.InvalidTokenException;
import com.aigo.speech.auth.jwt.PasswordResetTokenProvider;
import com.aigo.speech.mail.server.MailServer;
import com.aigo.speech.mail.service.MailTemplateService;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private MailServer mailServer;

    @Mock
    private MailTemplateService mailTemplateService;

    @Mock
    private PasswordResetTokenProvider passwordResetTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private static final String EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "RESET_EXPIRY_MINUTES", 30);
        ReflectionTestUtils.setField(passwordResetService, "BASE_URL", "http://localhost:3000");
    }

    // ======================== sendPasswordResetEmail ========================

    @Test
    @DisplayName("존재하지 않는 이메일로 요청 시 메일이 발송되지 않는다")
    void sendPasswordResetEmail_whenUserNotFound_doesNotSendMail() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        passwordResetService.sendPasswordResetEmail(EMAIL);

        verify(mailServer, never()).sendHtml(any(), any(), any());
    }

    @Test
    @DisplayName("존재하는 이메일로 요청 시 재설정 메일이 발송된다")
    void sendPasswordResetEmail_whenUserExists_sendsResetEmail() {
        User user = User.builder().email(EMAIL).password("encoded").username("tester").build();
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordResetTokenProvider.generateToken(EMAIL)).willReturn("reset-token");
        given(mailTemplateService.renderPasswordReset(anyString(), eq(30))).willReturn("<html/>");

        passwordResetService.sendPasswordResetEmail(EMAIL);

        verify(passwordResetTokenProvider).generateToken(EMAIL);
        verify(mailTemplateService).renderPasswordReset(
                contains("http://localhost:3000/reset-password?token=reset-token"), eq(30));
        verify(mailServer).sendHtml(eq(EMAIL), eq("[음어그] 비밀번호 재설정 안내"), anyString());
    }

    // ======================== resetPassword ========================

    @Test
    @DisplayName("유효하지 않은 토큰으로 비밀번호 재설정 시 예외가 발생한다")
    void resetPassword_withInvalidToken_throwsException() {
        given(passwordResetTokenProvider.validateAndGetEmail("bad-token"))
                .willThrow(new InvalidTokenException("유효하지 않은 토큰입니다."));

        assertThatThrownBy(() -> passwordResetService.resetPassword("bad-token", "newPass"))
                .isInstanceOf(InvalidTokenException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("토큰은 유효하지만 사용자가 없으면 예외가 발생한다")
    void resetPassword_withUnknownUser_throwsException() {
        given(passwordResetTokenProvider.validateAndGetEmail("valid-token")).willReturn(EMAIL);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token", "newPass"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

    @Test
    @DisplayName("유효한 토큰으로 비밀번호 재설정 시 새 비밀번호로 변경된다")
    void resetPassword_withValidToken_updatesPassword() {
        User user = User.builder().email(EMAIL).password("old_enc").username("tester").build();
        given(passwordResetTokenProvider.validateAndGetEmail("valid-token")).willReturn(EMAIL);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(bCryptPasswordEncoder.encode("newPass")).willReturn("new_enc");

        passwordResetService.resetPassword("valid-token", "newPass");

        assertThat(user.getPassword()).isEqualTo("new_enc");
        verify(userRepository).save(user);
    }

    // ======================== changePassword ========================

    @Test
    @DisplayName("현재 비밀번호가 일치하지 않으면 예외가 발생한다")
    void changePassword_withWrongCurrentPassword_throwsException() {
        User user = User.builder().email(EMAIL).password("enc").username("tester").build();
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(bCryptPasswordEncoder.matches("wrong", "enc")).willReturn(false);

        assertThatThrownBy(() -> passwordResetService.changePassword(EMAIL, "wrong", "newPass", "newPass"))
                .isInstanceOf(RuntimeException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("새 비밀번호와 확인 비밀번호가 다르면 예외가 발생한다")
    void changePassword_withMismatchedNewPasswords_throwsException() {
        User user = User.builder().email(EMAIL).password("enc").username("tester").build();
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(bCryptPasswordEncoder.matches("current", "enc")).willReturn(true);

        assertThatThrownBy(() -> passwordResetService.changePassword(EMAIL, "current", "newPass", "different"))
                .isInstanceOf(RuntimeException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("현재 비밀번호가 일치하면 새 비밀번호로 변경된다")
    void changePassword_withCorrectCurrentPassword_updatesPassword() {
        User user = User.builder().email(EMAIL).password("enc").username("tester").build();
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(bCryptPasswordEncoder.matches("current", "enc")).willReturn(true);
        given(bCryptPasswordEncoder.matches("newPass", "enc")).willReturn(false);
        given(bCryptPasswordEncoder.encode("newPass")).willReturn("new_enc");

        passwordResetService.changePassword(EMAIL, "current", "newPass", "newPass");

        assertThat(user.getPassword()).isEqualTo("new_enc");
        verify(userRepository).save(user);
    }
}
