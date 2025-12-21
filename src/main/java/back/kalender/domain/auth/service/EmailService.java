package back.kalender.domain.auth.service;

import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String BRAND_NAME = "Kalender";
    private static final String TEMPLATE_VERIFICATION = "email/verification";
    private static final String TEMPLATE_PASSWORD_RESET = "email/password-reset";
    private static final String CHARSET_UTF8 = "UTF-8";
    private static final String PASSWORD_RESET_PATH = "/auth/reset-password";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${custom.site.frontUrl}")
    private String frontUrl;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendVerificationEmail(String to, String code) {
        String subject = String.format("[%s] 이메일 인증 코드", BRAND_NAME);
        Map<String, Object> variables = Map.of(
                "code", code,
                "frontUrl", frontUrl
        );

        sendEmail(to, subject, TEMPLATE_VERIFICATION, variables);
        log.info("[Auth](이메일 인증 코드) 수신자: {} | 인증 코드: {}", to, code);
    }

    public void sendPasswordResetEmail(String to, String token) {
        String subject = String.format("[%s] 비밀번호 재설정", BRAND_NAME);
        String resetUrl = buildResetUrl(token);

        Map<String, Object> variables = Map.of(
                "resetUrl", resetUrl,
                "frontUrl", frontUrl
        );

        sendEmail(to, subject, TEMPLATE_PASSWORD_RESET, variables);
        log.info("[Auth](비밀번호 재설정) 수신자: {} | 토큰: {} | 링크: {}", to, token, resetUrl);
    }

    // 공통 이메일 발송 메서드
    private void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            MimeMessage message = createMimeMessage(to, subject, templateName, variables);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            log.error("이메일 발송 실패 - 수신자: {}, 제목: {}", to, subject, e);
            throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // MimeMessage 생성
    private MimeMessage createMimeMessage(String to, String subject, String templateName, Map<String, Object> variables)
            throws MessagingException {
        // fromEmail 검증
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            throw new MessagingException("이메일 발신자 주소가 설정되지 않았습니다. spring.mail.username을 설정해주세요.");
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, CHARSET_UTF8);

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);

        String htmlContent = renderTemplate(templateName, variables);
        helper.setText(htmlContent, true);

        return message;
    }

    // Thymeleaf 템플릿 렌더링
    private String renderTemplate(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            variables.forEach(context::setVariable);
            return templateEngine.process(templateName, context);
        } catch (Exception e) {
            log.error("템플릿 렌더링 실패 - 템플릿: {}", templateName, e);
            throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 비밀번호 재설정 URL 생성(인코딩/조합 안전)
    private String buildResetUrl(String token) {
        return UriComponentsBuilder.fromUriString(frontUrl)
                .path(PASSWORD_RESET_PATH)
                .queryParam("token", token)
                .build()
                .toUriString();
    }

}
