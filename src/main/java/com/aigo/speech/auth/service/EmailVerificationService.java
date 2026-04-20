package com.aigo.speech.auth.service;

import com.aigo.speech.mail.exception.MailVerificationException;
import com.aigo.speech.mail.server.MailServer;
import com.aigo.speech.mail.service.MailTemplateService;
import com.aigo.speech.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final MailServer mailServer;
    private final MailTemplateService mailTemplateService;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "email:verification:";
    private static final int AUTH_CODE_LENGTH = 6;
    private static final char[] CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final SecureRandom random = new SecureRandom();
    private static final String VERIFIED_PREFIX = "email:verified:"; // 이메일 수정 시 '이메일 인증 여부 확인용' 키 추가

    @Value("${spring.mail.auth-code-expiration-min}")
    private int VERIFY_EXPIRY_MINUTES;

    @Value("${app.base-url}")
    private String BASE_URL;

    // 이메일 인증 - 코드 방식
    @Async
    public void sendVerificationCode(String email) {
        String key = PREFIX + email;
        String code = generateCode();
        log.info(String.valueOf(VERIFY_EXPIRY_MINUTES));
        log.info(BASE_URL);

        redisTemplate.opsForValue().set(key, code, VERIFY_EXPIRY_MINUTES, TimeUnit.MINUTES);

        String subject = "[음어그] 이메일 인증 안내";
        String context = mailTemplateService.renderEmailVerificationCode(
                code, VERIFY_EXPIRY_MINUTES
        );

        mailServer.sendHtml(email, subject, context);
    }

    public void verifyCode(String email, String code) {
        String key = PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            throw new MailVerificationException("인증 코드가 만료되었습니다.");
        }
        if (!storedCode.equals(code)) {
            throw new MailVerificationException("인증번호가 일치하지 않습니다.");
        }

        redisTemplate.opsForValue().set(VERIFIED_PREFIX + email,"true", 5, TimeUnit.MINUTES); // 이메일 인증 확인 여부에 대한 키를 5분간 저장

        redisTemplate.delete(key);
    }

    public boolean isVerified(String email) { // 인증 여부 확인
        return Boolean.TRUE.equals(redisTemplate.hasKey(VERIFIED_PREFIX + email));
    }

    public void deleteVerifiedStatus(String email) { // 인증 여부 확인 후 삭제
        redisTemplate.delete(VERIFIED_PREFIX + email);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(AUTH_CODE_LENGTH);
        for (int i = 0; i < AUTH_CODE_LENGTH; i++) {
            sb.append(CHAR_POOL[random.nextInt(CHAR_POOL.length)]);
        }
        return sb.toString();
    }

}
