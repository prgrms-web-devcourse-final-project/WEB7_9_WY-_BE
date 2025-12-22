package back.kalender.domain.auth.controller;

import back.kalender.domain.auth.dto.request.*;
import back.kalender.domain.auth.dto.response.*;
import back.kalender.domain.auth.service.AuthService;
import back.kalender.global.common.constant.HttpHeaders;
import back.kalender.global.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "UserAuthController", description = "유저 인증 인가 API")
@RequiredArgsConstructor
public class UserAuthController implements UserAuthControllerSpec {

    private final AuthService authService;

    @PostMapping("/login")
    @Override
    public ResponseEntity<UserLoginResponse> login(
            @Valid @RequestBody UserLoginRequest request,
            HttpServletResponse response
    ) {
        Object[] result = authService.loginWithToken(request, response);
        UserLoginResponse loginResponse = (UserLoginResponse) result[0];
        String accessToken = (String) result[1];
        // ResponseEntity 헤더에 Authorization 직접 설정
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, HttpHeaders.createBearerToken(accessToken))
                .body(loginResponse);
    }

    @PostMapping("/logout")
    @Override
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken, response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Override
    public ResponseEntity<Void> refreshToken(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        String accessToken = authService.refreshToken(refreshToken, response);
        // ResponseEntity 헤더에 Authorization 직접 설정
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, HttpHeaders.createBearerToken(accessToken))
                .build();
    }

    @PostMapping("/password/send")
    @Override
    public ResponseEntity<Void> sendPasswordResetEmail(
            @Valid @RequestBody UserPasswordResetSendRequest request
    ) {
        authService.sendPasswordResetEmail(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset")
    @Override
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody UserPasswordResetRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/send")
    @Override
    public ResponseEntity<Void> sendVerifyEmail(
            @Valid @RequestBody VerifyEmailSendRequest request
    ) {
        authService.sendVerifyEmail(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/verify")
    @Override
    public ResponseEntity<VerifyEmailResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        VerifyEmailResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email")
    @Override
    public ResponseEntity<EmailStatusResponse> getEmailStatus() {
        Long userId = SecurityUtil.getCurrentUserIdOrThrow();
        
        EmailStatusResponse response = authService.getEmailStatus(userId);
        return ResponseEntity.ok(response);
    }
}
