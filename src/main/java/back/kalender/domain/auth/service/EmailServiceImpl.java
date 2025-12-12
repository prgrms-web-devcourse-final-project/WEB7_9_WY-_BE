package back.kalender.domain.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${custom.site.frontUrl}")
    private String frontUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendVerificationEmail(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("[Kalender] 이메일 인증 코드");

            // Thymeleaf 템플릿에 전달할 변수 설정
            Context context = new Context();
            context.setVariable("code", code);
            context.setVariable("frontUrl", frontUrl);

            // 템플릿을 HTML로 렌더링
            String htmlContent = templateEngine.process("email/verification", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("이메일 인증 코드 발송 완료: {}", to);
        } catch (MessagingException e) {
            log.error("이메일 인증 코드 발송 실패: {}", to, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("[Kalender] 비밀번호 재설정");

            // 비밀번호 재설정 링크 생성
            String resetUrl = frontUrl + "/auth/reset-password?token=" + token;

            // Thymeleaf 템플릿에 전달할 변수 설정
            Context context = new Context();
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("frontUrl", frontUrl);

            // 템플릿을 HTML로 렌더링
            String htmlContent = templateEngine.process("email/password-reset", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("비밀번호 재설정 이메일 발송 완료: {}", to);
        } catch (MessagingException e) {
            log.error("비밀번호 재설정 이메일 발송 실패: {}", to, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }
}

