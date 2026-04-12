package com.aigo.speech.mail.service;

import com.aigo.speech.mail.exception.MailVerificationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
public class MailVerificationService {

    private final MailService mailService;
    private final StringRedisTemplate redisTemplate;
    private final int expireMinutes;

    private static final String PREFIX = "email:verification:";

    private static final int AUTH_CODE_LENGTH = 6;
    private static final char[] CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final SecureRandom random = new SecureRandom();

    private static final String MAIL_VERIFICATION_TEMPLATE = """
        <table width="100%" cellpadding="0" cellspacing="0" style="padding:40px 0; background-color:#f5f7fa;">
          <tr>
            <td align="center">
              <table width="420" cellpadding="0" cellspacing="0" style="background:#ffffff; border-radius:12px; padding:30px;">
        
                <tr><td height="20"></td></tr>
        
                <tr>
                  <td style="font-size:14px; color:#555; line-height:1.6; font-family:Arial, sans-serif;">
                    안녕하세요, <b>음어그(U-U-G)</b>입니다.<br><br>
                    아래 인증번호를 입력하여 이메일 인증을 완료해주세요.
                  </td>
                </tr>
        
                <tr><td height="25"></td></tr>
        
                <tr>
                  <td align="center">
                    <table cellpadding="0" cellspacing="0">
                      <tr>
                        <td style="
                          padding:18px 32px;
                          background:#f1f5ff;
                          border:2px dashed #4a6cf7;
                          border-radius:10px;
                          font-size:26px;
                          font-weight:bold;
                          letter-spacing:6px;
                          color:#4a6cf7;
                          font-family:Arial, sans-serif;
                          text-align:center;
                        ">
                          {code}
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
        
                <tr><td height="25"></td></tr>
        
                <tr>
                  <td style="font-size:13px; color:#888; line-height:1.6; font-family:Arial, sans-serif;">
                    ※ 인증번호는 <b>{time}분 동안</b>만 유효합니다.<br>
                    ※ 본인이 요청하지 않은 경우 이 메일을 무시해주세요.
                  </td>
                </tr>
        
                <tr><td height="30"></td></tr>
        
                <tr>
                  <td style="font-size:13px; color:#aaa; font-family:Arial, sans-serif;">
                    감사합니다.<br>
                    <b>음어그(U-U-G)</b> 드림
                  </td>
                </tr>
        
              </table>
            </td>
          </tr>
        </table>
        """;

    public MailVerificationService(MailService mailService, StringRedisTemplate redisTemplate, @Value("${spring.mail.auth-code-expiration-min}") int expireMinutes) {
        this.mailService = mailService;
        this.redisTemplate = redisTemplate;
        this.expireMinutes = expireMinutes;
    }

    private static final String PASSWORD_VERIFICATION_TEMPLATE = """
            비밀번호 재설정을 요청하셨습니다.
                        아래 링크를 클릭하여 비밀번호를 변경해 주세요. (1시간 유효)
            
                        https://example.com/reset-password?token={token}
            
                        본인이 요청하지 않은 경우 이 메일을 무시하세요.
            """;

    public void sendEmailVerificationCode(String email) {
        String key = PREFIX + email;
        String code = generateCode();

        redisTemplate.opsForValue().set(key, code, EXPIRE_TIME, TimeUnit.MINUTES);

        String subject = "[음어그] 이메일 인증 안내";

        String content = buildEmailContent(code);

        mailService.sendHtmlMail(email, subject, content);
    }

    public void verifyCode(String email, String code) {
        String key = PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            throw new MailVerificationException("인증 코드가 만료되었습니다.");
        }
        if (!storedCode.equals(code)) {
            throw new MailVerificationException("인증 코드가 일치하지 않습니다.");
        }

        redisTemplate.delete(key);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(AUTH_CODE_LENGTH);
        for (int i = 0; i < AUTH_CODE_LENGTH; i++) {
            sb.append(CHAR_POOL[random.nextInt(CHAR_POOL.length)]);
        }
        return sb.toString();
    }

    private String buildEmailContent(String code) {
        return MAIL_VERIFICATION_TEMPLATE
                .replace("{code}", code)
                .replace("{time}", String.valueOf(EXPIRE_TIME));
    }
}

