package back.kalender.domain.auth.controller;

import back.kalender.domain.auth.dto.request.*;
import back.kalender.domain.auth.dto.response.*;
import back.kalender.domain.auth.service.AuthService;
import back.kalender.global.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
public class UserAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다. 성공 시 access token은 Authorization 헤더로, refresh token은 httpOnly secure 쿠키로 전달됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = UserLoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "BAD_REQUEST",
                                "status": "400",
                                "message": "잘못된 요청입니다."
                              }
                            }
                            """))),
            @ApiResponse(responseCode = "401", description = "로그인 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "INVALID_CREDENTIALS",
                                "status": "401",
                                "message": "이메일 또는 비밀번호가 올바르지 않습니다."
                              }
                            }
                            """)))
    })
    public ResponseEntity<UserLoginResponse> login(
            @Valid @RequestBody UserLoginRequest request,
            HttpServletResponse response
    ) {
        UserLoginResponse loginResponse = authService.login(request, response);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Authorization 헤더의 access token과 쿠키의 refresh token을 무효화하고 로그아웃합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "UNAUTHORIZED",
                                "status": "401",
                                "message": "로그인이 필요합니다."
                              }
                            }
                            """)))
    })
    public ResponseEntity<Void> logout(
            @Parameter(description = "Refresh Token (httpOnly secure 쿠키)", hidden = false)
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken, response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "httpOnly secure 쿠키에 담긴 refresh token으로 새로운 access token과 refresh token을 발급받습니다. 새 access token은 Authorization 헤더로, 새 refresh token은 httpOnly secure 쿠키로 전달됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @ApiResponse(responseCode = "401", description = "토큰 갱신 실패",
                    content = @Content(examples = {
                            @ExampleObject(name = "유효하지 않은 토큰", value = """
                                    {
                                      "error": {
                                        "code": "INVALID_REFRESH_TOKEN",
                                        "status": "401",
                                        "message": "유효하지 않은 refresh token입니다."
                                      }
                                    }
                                    """),
                            @ExampleObject(name = "만료된 토큰", value = """
                                    {
                                      "error": {
                                        "code": "EXPIRED_REFRESH_TOKEN",
                                        "status": "401",
                                        "message": "만료된 refresh token입니다."
                                      }
                                    }
                                    """)
                    }))
    })
    public ResponseEntity<Void> refreshToken(
            @Parameter(description = "Refresh Token (httpOnly secure 쿠키)", hidden = false)
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.refreshToken(refreshToken, response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/send")
    @Operation(summary = "비밀번호 재설정 이메일 발송", description = "비밀번호 재설정을 위한 이메일을 발송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 발송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "BAD_REQUEST",
                                "status": "400",
                                "message": "잘못된 요청입니다."
                              }
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "USER_NOT_FOUND",
                                "status": "404",
                                "message": "유저를 찾을 수 없습니다."
                              }
                            }
                            """)))
    })
    public ResponseEntity<Void> sendPasswordResetEmail(
            @Valid @RequestBody UserPasswordResetSendRequest request
    ) {
        authService.sendPasswordResetEmail(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset")
    @Operation(summary = "비밀번호 재설정", description = "이메일로 받은 토큰을 사용하여 비밀번호를 재설정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 재설정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(examples = {
                            @ExampleObject(name = "유효하지 않은 토큰", value = """
                                    {
                                      "error": {
                                        "code": "INVALID_PASSWORD_RESET_TOKEN",
                                        "status": "400",
                                        "message": "유효하지 않은 비밀번호 재설정 토큰입니다."
                                      }
                                    }
                                    """),
                            @ExampleObject(name = "비밀번호 불일치", value = """
                                    {
                                      "error": {
                                        "code": "PASSWORD_MISMATCH",
                                        "status": "400",
                                        "message": "비밀번호가 일치하지 않습니다."
                                      }
                                    }
                                    """),
                            @ExampleObject(name = "이미 사용된 토큰", value = """
                                    {
                                      "error": {
                                        "code": "PASSWORD_RESET_TOKEN_ALREADY_USED",
                                        "status": "400",
                                        "message": "이미 사용된 비밀번호 재설정 토큰입니다."
                                      }
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "401", description = "만료된 토큰",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "EXPIRED_PASSWORD_RESET_TOKEN",
                                "status": "401",
                                "message": "만료된 비밀번호 재설정 토큰입니다."
                              }
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "토큰을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "PASSWORD_RESET_TOKEN_NOT_FOUND",
                                "status": "404",
                                "message": "비밀번호 재설정 토큰을 찾을 수 없습니다."
                              }
                            }
                            """)))
    })
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody UserPasswordResetRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/send")
    @Operation(summary = "이메일 인증 코드 발송", description = "이메일 인증을 위한 인증 코드를 발송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 코드 발송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(examples = {
                            @ExampleObject(name = "잘못된 요청", value = """
                                    {
                                      "error": {
                                        "code": "BAD_REQUEST",
                                        "status": "400",
                                        "message": "잘못된 요청입니다."
                                      }
                                    }
                                    """),
                            @ExampleObject(name = "이미 인증된 이메일", value = """
                                    {
                                      "error": {
                                        "code": "EMAIL_ALREADY_VERIFIED",
                                        "status": "400",
                                        "message": "이미 인증된 이메일입니다."
                                      }
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "USER_NOT_FOUND",
                                "status": "404",
                                "message": "유저를 찾을 수 없습니다."
                              }
                            }
                            """))),
            @ApiResponse(responseCode = "429", description = "너무 많은 요청",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "EMAIL_VERIFICATION_LIMIT_EXCEEDED",
                                "status": "429",
                                "message": "인증 코드 발송 횟수를 초과했습니다."
                              }
                            }
                            """)))
    })
    public ResponseEntity<Void> sendVerifyEmail(
            @Valid @RequestBody VerifyEmailSendRequest request
    ) {
        authService.sendVerifyEmail(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/verify")
    @Operation(summary = "이메일 인증 확인", description = "발송된 인증 코드를 확인하여 이메일을 인증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 인증 성공",
                    content = @Content(schema = @Schema(implementation = VerifyEmailResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "INVALID_EMAIL_VERIFICATION_CODE",
                                "status": "400",
                                "message": "유효하지 않은 인증 코드입니다."
                              }
                            }
                            """))),
            @ApiResponse(responseCode = "401", description = "만료된 인증 코드",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "EXPIRED_EMAIL_VERIFICATION_CODE",
                                "status": "401",
                                "message": "만료된 인증 코드입니다."
                              }
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "찾을 수 없음",
                    content = @Content(examples = {
                            @ExampleObject(name = "인증 코드를 찾을 수 없음", value = """
                                    {
                                      "error": {
                                        "code": "EMAIL_VERIFICATION_CODE_NOT_FOUND",
                                        "status": "404",
                                        "message": "인증 코드를 찾을 수 없습니다."
                                      }
                                    }
                                    """),
                            @ExampleObject(name = "유저를 찾을 수 없음", value = """
                                    {
                                      "error": {
                                        "code": "USER_NOT_FOUND",
                                        "status": "404",
                                        "message": "유저를 찾을 수 없습니다."
                                      }
                                    }
                                    """)
                    }))
    })
    public ResponseEntity<VerifyEmailResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        VerifyEmailResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email")
    @Operation(summary = "이메일 인증 상태 확인", description = "유저의 이메일 인증 상태를 조회합니다. Authorization 헤더의 access token이 필요합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = EmailStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "UNAUTHORIZED",
                                "status": "401",
                                "message": "로그인이 필요합니다."
                              }
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "USER_NOT_FOUND",
                                "status": "404",
                                "message": "유저를 찾을 수 없습니다."
                              }
                            }
                            """)))
    })
    public ResponseEntity<EmailStatusResponse> getEmailStatus() {
        Long userId = SecurityUtil.getCurrentUserIdOrThrow();
        
        EmailStatusResponse response = authService.getEmailStatus(userId);
        return ResponseEntity.ok(response);
    }
}
