package back.kalender.domain.auth.service;

public interface EmailService {
    /**
     * 이메일 인증 코드 발송
     * @param to 수신자 이메일
     * @param code 인증 코드 (6자리)
     */
    void sendVerificationEmail(String to, String code);

    /**
     * 비밀번호 재설정 이메일 발송
     * @param to 수신자 이메일
     * @param token 비밀번호 재설정 토큰
     */
    void sendPasswordResetEmail(String to, String token);
}

