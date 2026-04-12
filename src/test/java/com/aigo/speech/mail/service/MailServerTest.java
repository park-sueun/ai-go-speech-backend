package com.aigo.speech.mail.service;

import com.aigo.speech.auth.service.EmailVerificationService;
import com.aigo.speech.mail.exception.MailVerificationException;
import com.aigo.speech.mail.server.MailServer;
import com.aigo.speech.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private MailServer mailServer;

    @Mock
    private MailTemplateService mailTemplateService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private static final String PREFIX = "email:verification:";
    private static final String EMAIL = "user@example.com";
    private static final String REDIS_KEY = PREFIX + EMAIL;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailVerificationService, "VERIFY_EXPIRY_MINUTES", 3);
        ReflectionTestUtils.setField(emailVerificationService, "BASE_URL", "http://localhost:3000");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Test
    @DisplayName("인증 코드 발송 시 Redis에 코드가 저장되고 HTML 메일이 발송된다")
    void sendVerificationCode_savesCodeToRedisAndSendsMail() {
        given(mailTemplateService.renderEmailVerificationCode(anyString(), eq(3)))
                .willReturn("<html>code</html>");

        emailVerificationService.sendVerificationCode(EMAIL);

        verify(valueOperations).set(eq(REDIS_KEY), anyString(), eq(3L), eq(TimeUnit.MINUTES));
        verify(mailTemplateService).renderEmailVerificationCode(anyString(), eq(3));
        verify(mailServer).sendHtml(eq(EMAIL), eq("[음어그] 이메일 인증 안내"), anyString());
    }

    @Test
    @DisplayName("올바른 인증 코드 입력 시 검증에 성공하고 Redis 키가 삭제된다")
    void verifyCode_withCorrectCode_succeedsAndDeletesKey() {
        String code = "AZ45IK";
        given(valueOperations.get(REDIS_KEY)).willReturn(code);

        assertDoesNotThrow(() -> emailVerificationService.verifyCode(EMAIL, code));

        verify(redisTemplate).delete(REDIS_KEY);
    }

    @Test
    @DisplayName("잘못된 인증 코드 입력 시 예외가 발생한다")
    void verifyCode_withWrongCode_throwsException() {
        given(valueOperations.get(REDIS_KEY)).willReturn("AZ45IK");

        assertThatThrownBy(() -> emailVerificationService.verifyCode(EMAIL, "WRONG1"))
                .isInstanceOf(MailVerificationException.class)
                .hasMessage("인증 코드가 일치하지 않습니다.");

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("만료된 인증 코드 입력 시 예외가 발생한다")
    void verifyCode_whenCodeExpired_throwsException() {
        given(valueOperations.get(REDIS_KEY)).willReturn(null);

        assertThatThrownBy(() -> emailVerificationService.verifyCode(EMAIL, "AZ45IK"))
                .isInstanceOf(MailVerificationException.class)
                .hasMessage("인증 코드가 만료되었습니다.");
    }
}
