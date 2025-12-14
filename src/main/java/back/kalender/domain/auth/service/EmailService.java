package back.kalender.domain.auth.service;

public interface EmailService {
    // 이메일 인증 코드 발송
    void sendVerificationEmail(String to, String code);

    // 비밀번호 재설정 이메일 발송    
    void sendPasswordResetEmail(String to, String token);
}

