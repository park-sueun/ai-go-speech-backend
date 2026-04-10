package com.aigo.speech.mail.service;

import com.aigo.speech.mail.exception.MailVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private MailService mailService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private MailVerificationService mailVerificationService;

    private static final String PREFIX = "email:verification:";
    private static final String EMAIL = "user@example.com";
    private static final String REDIS_KEY = PREFIX + EMAIL;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Test
    @DisplayName("인증 코드 전송 시 Redis에 코드가 저장되고 메일이 발송된다")
    void sendVerificationCode_savesCodeToRedisAndSendsMail() {
        mailVerificationService.sendVerificationCode(EMAIL);

        verify(valueOperations).set(eq(REDIS_KEY), anyString(), anyLong(), eq(TimeUnit.MINUTES));
        verify(mailService).sendHtmlMail(eq(EMAIL), anyString(), anyString());
    }

    @Test
    @DisplayName("올바른 인증 코드 입력 시 검증에 성공한다")
    void verifyCode_withCorrectCode_succeeds() {
        String code = "AZ45IK";
        given(valueOperations.getAndDelete(REDIS_KEY)).willReturn(code);

        assertDoesNotThrow(() -> mailVerificationService.verifyCode(EMAIL, code));
    }

    @Test
    @DisplayName("잘못된 인증 코드 입력 시 예외가 발생한다")
    void verifyCode_withWrongCode_throwsException() {
        given(valueOperations.getAndDelete(REDIS_KEY)).willReturn("AZ45IK");

        assertThatThrownBy(() -> mailVerificationService.verifyCode(EMAIL, "WRONG1"))
                .isInstanceOf(MailVerificationException.class)
                .hasMessage("인증 코드가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("만료된 인증 코드 입력 시 예외가 발생한다")
    void verifyCode_whenCodeExpired_throwsException() {
        given(valueOperations.getAndDelete(REDIS_KEY)).willReturn(null);

        assertThatThrownBy(() -> mailVerificationService.verifyCode(EMAIL, "AZ45IK"))
                .isInstanceOf(MailVerificationException.class)
                .hasMessage("인증 코드가 만료되었습니다.");
    }
}
