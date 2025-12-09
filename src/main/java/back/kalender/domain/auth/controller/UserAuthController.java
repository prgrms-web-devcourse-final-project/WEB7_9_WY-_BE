package back.kalender.domain.auth.controller;

import back.kalender.domain.auth.dto.request.*;
import back.kalender.domain.auth.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "UserAuthController", description = "유저 인증 인가 API")
public class UserAuthController {

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다. 성공 시 access token은 Authorization 헤더로, refresh token은 httpOnly secure 쿠키로 전달됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이메일 형식 오류, 필수 값 누락)"),
            @ApiResponse(responseCode = "401", description = "로그인 실패 (값 불일치)")
    })
    public ResponseEntity<UserLoginResponse> login(
            @Valid @RequestBody UserLoginRequest request
    ) {
        // 항상 성공 반환
        // 실제 구현 시 -> access token은 Response Header의 Authorization에 설정, refresh token은 httpOnly secure 쿠키로 설정
        UserLoginResponse response = new UserLoginResponse(
                1L,
                "홍길동",
                request.email(),
                "test.com/profile.jpg",
                true
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Authorization 헤더의 access token과 쿠키의 refresh token을 무효화하고 로그아웃합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<Void> logout(
            @Parameter(description = "Access Token (Authorization 헤더)", hidden = false)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "Refresh Token (httpOnly secure 쿠키)", hidden = false)
            @CookieValue(value = "refreshToken", required = false) String refreshToken
    ) {
        // 항상 성공 반환
        // 실제 구현 시 -> Authorization 헤더에서 Bearer 토큰 추출하여 무효화, refresh token 쿠키 삭제 필요
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "httpOnly secure 쿠키에 담긴 refresh token으로 새로운 access token과 refresh token을 발급받습니다. 새 access token은 Authorization 헤더로, 새 refresh token은 httpOnly secure 쿠키로 전달됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @ApiResponse(responseCode = "401", description = "토큰 갱신 실패 (유효하지 않은 refresh token)")
    })
    public ResponseEntity<Void> refreshToken(
            @Parameter(description = "Refresh Token (httpOnly secure 쿠키)", hidden = false)
            @CookieValue(value = "refreshToken", required = false) String refreshToken
    ) {
        // 항상 성공 반환
        // 실제 구현 시 -> 새 access token은 Response Header의 Authorization에 설정, 새 refresh token은 httpOnly secure 쿠키로 설정
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/send")
    @Operation(summary = "비밀번호 재설정 이메일 발송", description = "비밀번호 재설정을 위한 이메일을 발송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 발송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이메일 형식 오류)"),
            @ApiResponse(responseCode = "404", description = "해당 이메일로 가입된 유저를 찾을 수 없음")
    })
    public ResponseEntity<Void> sendPasswordResetEmail(
            @Valid @RequestBody UserPasswordResetSendRequest request
    ) {
        // 항상 성공 반환
        // 실제 구현 시 -> 토큰 생성 및 이메일 발송 로직 필요
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset")
    @Operation(summary = "비밀번호 재설정", description = "이메일로 받은 토큰을 사용하여 비밀번호를 재설정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 재설정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (토큰 오류, 비밀번호 형식 오류, 비밀번호 불일치)"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰 또는 만료된 토큰")
    })
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody UserPasswordResetRequest request
    ) {
        // 항상 성공 반환
        // 실제 구현 시 -> 토큰 검증 및 비밀번호 업데이트 로직 필요
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/send")
    @Operation(summary = "이메일 인증 코드 발송", description = "이메일 인증을 위한 인증 코드를 발송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 코드 발송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이메일 형식 오류)"),
            @ApiResponse(responseCode = "429", description = "너무 많은 요청 (인증 코드 재발송 제한)")
    })
    public ResponseEntity<Void> sendVerifyEmail(
            @Valid @RequestBody VerifyEmailSendRequest request
    ) {
        // 항상 성공 반환
        // 실제 구현 시-> 인증 코드 생성 및 이메일 발송 로직 필요
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/verify")
    @Operation(summary = "이메일 인증 확인", description = "발송된 인증 코드를 확인하여 이메일을 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 인증 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (인증 코드 오류, 이메일 형식 오류)"),
            @ApiResponse(responseCode = "401", description = "인증 코드 불일치 또는 만료")
    })
    public ResponseEntity<VerifyEmailResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        // 항상 성공 반환
        // 실제 구현 시 -> 인증 코드 검증 로직 필요
        VerifyEmailResponse response = new VerifyEmailResponse(
                request.email(),
                true
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email")
    @Operation(summary = "이메일 인증 상태 확인", description = "유저의 이메일 인증 상태를 조회합니다. Authorization 헤더의 access token이 필요합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음")
    })
    public ResponseEntity<EmailStatusResponse> getEmailStatus(
            @Parameter(description = "Access Token (Authorization 헤더)", hidden = false)
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        // 항상 성공 반환
        // 실제 구현 시 -> Authorization 헤더에서 Bearer 토큰 추출하여 JWT 파싱, 유저 정보 추출 후 DB에서 인증 상태 조회
        EmailStatusResponse response = new EmailStatusResponse(
                1L,
                "user@example.com",
                true,
                java.time.LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
}
