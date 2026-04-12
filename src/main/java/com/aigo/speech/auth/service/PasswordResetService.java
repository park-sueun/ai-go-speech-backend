package com.aigo.speech.auth.service;

import com.aigo.speech.auth.dto.ChangePasswordRequest;
import com.aigo.speech.auth.exception.InvalidPasswordException;
import com.aigo.speech.auth.exception.PasswordMismatchException;
import com.aigo.speech.auth.exception.SamePasswordException;
import com.aigo.speech.auth.jwt.PasswordResetTokenProvider;
import com.aigo.speech.mail.server.MailServer;
import com.aigo.speech.mail.service.MailTemplateService;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final MailServer mailServer;
    private final MailTemplateService mailTemplateService;
    private final PasswordResetTokenProvider passwordResetTokenProvider;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Value("${jwt.reset-expiration-min}")
    private int RESET_EXPIRY_MINUTES;

    @Value("${app.base-url}")
    private String BASE_URL;

    @Async("mailExecutor")
    public void sendPasswordResetEmail(String email) {
        userRepository.findByEmail(email).ifPresent(user ->{
            String token = passwordResetTokenProvider.generateToken(email);
            String resetUrl = BASE_URL + "/reset-password?token=" + token;

            // 템플릿 렌더링
            String html = mailTemplateService.renderPasswordReset(
                    resetUrl, RESET_EXPIRY_MINUTES
            );

            // HTML 메일 발송
            mailServer.sendHtml(email, "[음어그] 비밀번호 재설정 안내", html);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        // 1. 토큰에 담긴 이메일 정보 확인
        String email = passwordResetTokenProvider.validateAndGetEmail(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자입니다."));

        // 2. 새 비밀번호 암호화 후 변경
        user.changePassword(bCryptPasswordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("[PasswordResetService] 비밀번호 초기화 완료 → {}", email);
    }

    /**
     * 로그인 유저 비밀번호 변경
     * @param user
     * @param currentPassword
     * @param newPassword
     * @param confirmPassword
     */
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword, String confirmPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자입니다."));

        // 현재 비밀번호 일치 여부 검증
        if (!bCryptPasswordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidPasswordException("현재 비밀번호가 올바르지 않습니다.");
        }

        // 비밀번호 확인 검증
        if (!newPassword.equals(confirmPassword)) {
            throw new PasswordMismatchException("새 비밀번호가 일치하지 않습니다.");
        }

        // 현재 비밀번호와 새 비밀번호 동일 여부 검증
        if (bCryptPasswordEncoder.matches(newPassword, user.getPassword())) {
            throw new SamePasswordException("현재 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.");
        }

        user.changePassword(bCryptPasswordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("[PasswordChangeService] 비밀번호 변경 완료 → {}", email);
    }
}
