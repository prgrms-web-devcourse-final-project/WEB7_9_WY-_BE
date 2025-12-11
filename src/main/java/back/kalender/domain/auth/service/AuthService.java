package back.kalender.domain.auth.service;

import back.kalender.domain.auth.dto.request.*;
import back.kalender.domain.auth.dto.response.EmailStatusResponse;
import back.kalender.domain.auth.dto.response.UserLoginResponse;
import back.kalender.domain.auth.dto.response.VerifyEmailResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    UserLoginResponse login(UserLoginRequest request, HttpServletResponse response);
    void logout(String refreshToken);
    void refreshToken(String refreshToken, HttpServletResponse response);
    void sendPasswordResetEmail(UserPasswordResetSendRequest request);
    void resetPassword(UserPasswordResetRequest request);
    void sendVerifyEmail(VerifyEmailSendRequest request);
    VerifyEmailResponse verifyEmail(VerifyEmailRequest request);
    EmailStatusResponse getEmailStatus(Long userId);
}

