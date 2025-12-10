package back.kalender.domain.user.controller;

import back.kalender.domain.user.dto.request.UpdateProfileRequest;
import back.kalender.domain.user.dto.request.UserSignupRequest;
import back.kalender.domain.user.dto.response.UploadProfileImgResponse;
import back.kalender.domain.user.dto.response.UserProfileResponse;
import back.kalender.domain.user.dto.response.UserSignupResponse;
import back.kalender.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Tag(name = "User", description = "회원가입, 회원 정보 관련 API")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(
            summary = "회원가입",
            description = "새로운 사용자를 등록합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = UserSignupResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "필수 값 누락",
                                            value = """
                                                {
                                                  "error": {
                                                    "code": "BAD_REQUEST",
                                                    "status": "400",
                                                    "message": "잘못된 요청입니다."
                                                  }
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "이메일 형식 오류",
                                            value = """
                                                {
                                                  "error": {
                                                    "code": "BAD_REQUEST",
                                                    "status": "400",
                                                    "message": "잘못된 요청입니다."
                                                  }
                                                }
                                                """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "중복된 데이터",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "DUPLICATE_NICKNAME",
                                            "status": "409",
                                            "message": "이미 사용 중인 닉네임입니다."
                                          }
                                        }
                                        """
                            )
                    )
            )
    })
    @PostMapping
    public ResponseEntity<UserSignupResponse> signup(
            @RequestBody UserSignupRequest request
    ) {
        // TODO: 더미데이터 삭제, 서비스 연결 필요

        // 더미 데이터
        UserSignupResponse dummyData = new UserSignupResponse(
                1L,
                request.email(),
                request.nickname(),
                request.birthDate(),
                LocalDateTime.now()
        );

        return ResponseEntity.ok(dummyData);
    }


    @Operation(
            summary = "내 정보 조회",
            description = "로그인한 사용자의 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "UNAUTHORIZED",
                                            "status": "401",
                                            "message": "로그인이 필요합니다."
                                          }
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "USER_NOT_FOUND",
                                            "status": "404",
                                            "message": "유저를 찾을 수 없습니다."
                                          }
                                        }
                                        """
                            )
                    )
            )
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        // TODO: @AuthenticationPrincipal로 userId 받아오기
        Long userId = 1L;

        UserProfileResponse response = userService.getMyProfile(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "프로필 이미지 업로드",
            description = "사용자의 프로필 이미지를 업로드합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "업로드 성공",
                    content = @Content(schema = @Schema(implementation = UploadProfileImgResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 파일",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "BAD_REQUEST",
                                            "status": "400",
                                            "message": "잘못된 요청입니다."
                                          }
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "UNAUTHORIZED",
                                            "status": "401",
                                            "message": "로그인이 필요합니다."
                                          }
                                        }
                                        """
                            )
                    )
            )
    })
    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadProfileImgResponse> uploadProfileImage(
            @RequestParam("profile_image") MultipartFile profileImage
    ) {
        // TODO: @AuthenticationPrincipal로 userId 받아오기
        Long userId = 1L;

        UploadProfileImgResponse response = userService.uploadProfileImage(userId, profileImage);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "내 정보 수정",
            description = "사용자의 닉네임, 프로필 이미지를 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "BAD_REQUEST",
                                            "status": "400",
                                            "message": "잘못된 요청입니다."
                                          }
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "UNAUTHORIZED",
                                            "status": "401",
                                            "message": "로그인이 필요합니다."
                                          }
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "닉네임 중복",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "DUPLICATE_NICKNAME",
                                            "status": "409",
                                            "message": "이미 사용 중인 닉네임입니다."
                                          }
                                        }
                                        """
                            )
                    )
            )
    })
    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @RequestBody UpdateProfileRequest request
    ) {
        // TODO: @AuthenticationPrincipal로 userId 받아오기
        Long userId = 1L;

        UserProfileResponse response = userService.updateMyProfile(userId, request);
        return ResponseEntity.ok(response);
    }
}
