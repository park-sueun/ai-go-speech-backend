package com.aigo.speech.mail.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class MailTemplateService {

    private final SpringTemplateEngine templateEngine;

    /**
     * 이메일 인증 HTML 렌더링 - 인증 코드
     * @param code 인증 코드
     * @param expiryMinutes 만료 시간(분)
     */
    public String renderEmailVerificationCode(String code, int expiryMinutes) {
        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("expiryMinutes", expiryMinutes);

        return templateEngine.process("mail/email-verification", context);
    }

    /**
     * 비밀번호 재설정 HTML 렌더링
     * @param resetUrl  재설정 링크
     * @param expiryMinutes 만료 시간(분)
     */
    public String renderPasswordReset(String resetUrl, int expiryMinutes) {
        Context context = new Context();
        context.setVariable("resetUrl", resetUrl);
        context.setVariable("expiryMinutes", expiryMinutes);

        return templateEngine.process("mail/password-reset", context);
    }

}
