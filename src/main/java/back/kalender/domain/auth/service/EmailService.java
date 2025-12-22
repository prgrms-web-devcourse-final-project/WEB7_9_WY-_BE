package back.kalender.domain.auth.service;

import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Arrays;
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
    private final Environment environment;

    @Value("${custom.site.frontUrl}")
    private String frontUrl;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    /**
     * 개발 환경인지 확인
     */
    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    public void sendVerificationEmail(String to, String code) {
        String subject = String.format("[%s] 이메일 인증 코드", BRAND_NAME);
        Map<String, Object> variables = Map.of(
                "code", code,
                "frontUrl", frontUrl
        );

        sendEmail(to, subject, TEMPLATE_VERIFICATION, variables);
        if (isDevProfile()) {
            log.info("[Auth](이메일 인증 코드) 수신자: {} | 인증 코드: {}", to, code);
        } else {
            log.info("[Auth](이메일 인증 코드) 수신자: {}", to);
        }
    }

    public void sendPasswordResetEmail(String to, String token) {
        String subject = String.format("[%s] 비밀번호 재설정", BRAND_NAME);
        String resetUrl = buildResetUrl(token);

        Map<String, Object> variables = Map.of(
                "resetUrl", resetUrl,
                "frontUrl", frontUrl
        );

        sendEmail(to, subject, TEMPLATE_PASSWORD_RESET, variables);
        if (isDevProfile()) {
            log.info("[Auth](비밀번호 재설정) 수신자: {} | 토큰: {} | 링크: {}", to, token, resetUrl);
        } else {
            log.info("[Auth](비밀번호 재설정) 수신자: {} | 링크: {}", to, resetUrl);
        }
    }

    // 공통 이메일 발송 메서드
    private void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        // 이메일 주소 유효성 검증
        validateEmailAddress(to);

        try {
            MimeMessage message = createMimeMessage(to, subject, templateName, variables);
            mailSender.send(message);
        } catch (MailAuthenticationException e) {
            log.error("이메일 서버 인증 실패 - 수신자: {}, 제목: {}", to, subject, e);
            throw new ServiceException(ErrorCode.EMAIL_CONFIGURATION_ERROR);
        } catch (MailSendException e) {
            log.error("이메일 발송 실패 (SMTP 오류) - 수신자: {}, 제목: {}", to, subject, e);
            throw new ServiceException(ErrorCode.EMAIL_SEND_FAILED);
        } catch (MailException e) {
            log.error("이메일 발송 실패 - 수신자: {}, 제목: {}", to, subject, e);
            throw new ServiceException(ErrorCode.EMAIL_SEND_FAILED);
        } catch (MessagingException e) {
            log.error("이메일 메시지 생성 실패 - 수신자: {}, 제목: {}", to, subject, e);
            throw new ServiceException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    // 이메일 주소 유효성 검증
    private void validateEmailAddress(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ServiceException(ErrorCode.INVALID_EMAIL_ADDRESS);
        }

        try {
            InternetAddress internetAddress = new InternetAddress(email);
            internetAddress.validate();
        } catch (jakarta.mail.internet.AddressException e) {
            log.warn("유효하지 않은 이메일 주소: {}", email);
            throw new ServiceException(ErrorCode.INVALID_EMAIL_ADDRESS);
        }
    }

    // MimeMessage 생성
    private MimeMessage createMimeMessage(String to, String subject, String templateName, Map<String, Object> variables)
            throws MessagingException {
        // fromEmail 검증
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            log.error("이메일 발신자 주소가 설정되지 않음 - spring.mail.username 설정 필요");
            throw new ServiceException(ErrorCode.EMAIL_CONFIGURATION_ERROR);
        }

        // fromEmail 유효성 검증
        try {
            InternetAddress internetAddress = new InternetAddress(fromEmail);
            internetAddress.validate();
        } catch (jakarta.mail.internet.AddressException e) {
            log.error("유효하지 않은 발신자 이메일 주소: {}", fromEmail);
            throw new ServiceException(ErrorCode.EMAIL_CONFIGURATION_ERROR);
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
            throw new ServiceException(ErrorCode.EMAIL_TEMPLATE_ERROR);
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
