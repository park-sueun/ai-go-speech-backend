package com.aigo.speech.mail.server;

import com.aigo.speech.mail.exception.MailSendException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailServer {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    // 텍스트 메일(Async)
    @Async("mailExecutor")
    public void sendText(String to, String subject, String text) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        try {
            mailSender.send(message);
            log.info("[MailServer] 텍스트 메일 발송 완료 → {}", to);
        } catch (MailException e) {
            log.error("[MailServer] 텍스트 메일 발송 실패 → {}", to, e);
            throw new MailSendException("텍스트 메일 발송에 실패했습니다.");
        }
    }

    // HTML 메일 (비동기)
    @Async("mailExecutor")
    public void sendHtml(String to, String subject, String htmlContent) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            // TODO: TTL
            log.info("[MailServer] HTML 메일 발송 완료 → {}", to);
        } catch (MessagingException | MailException e) {
            log.error("[MailServer] HTML 메일 발송 실패 → {}", to, e);
            throw new MailSendException("HTML 메일 발송에 실패했습니다.");
        }
    }
}