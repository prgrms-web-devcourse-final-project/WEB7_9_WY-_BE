package back.kalender.domain.user.controller;

import back.kalender.domain.user.dto.request.CompleteProfileImageRequest;
import back.kalender.domain.user.dto.request.UpdateProfileRequest;
import back.kalender.domain.user.dto.request.UserSignupRequest;
import back.kalender.domain.user.dto.response.PresignProfileImageResponse;
import back.kalender.domain.user.dto.response.UploadProfileImgResponse;
import back.kalender.domain.user.dto.response.UserProfileResponse;
import back.kalender.domain.user.dto.response.UserSignupResponse;
import back.kalender.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User", description = "회원가입, 회원 정보 관련 API")
public interface UserControllerSpec {

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
                            examples = {
                                    @ExampleObject(
                                            name = "이메일 중복",
                                            value = """
                                                {
                                                  "error": {
                                                    "code": "DUPLICATE_EMAIL",
                                                    "status": "409",
                                                    "message": "이미 사용 중인 이메일입니다."
                                                  }
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "닉네임 중복",
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
                            }
                    )
            )
    })
    ResponseEntity<UserSignupResponse> signup(@RequestBody UserSignupRequest request);

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
    ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails

    );

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
    ResponseEntity<UserProfileResponse> updateMyProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "프로필 이미지 업로드 presigned PUT 발급",
            description = "S3에 직접 업로드할 수 있는 presigned PUT URL을 발급합니다. contentType은 PUT 요청의 Content-Type과 반드시 동일해야 합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "발급 성공",
                    content = @Content(schema = @Schema(implementation = PresignProfileImageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(mediaType = "application/json")
            )
    })
    ResponseEntity<PresignProfileImageResponse> presignProfileImage(
            @RequestParam("contentType") String contentType,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "프로필 이미지 업로드 완료 처리",
            description = "클라이언트가 presigned PUT으로 S3 업로드를 완료한 뒤, 서버에 key를 저장합니다. 등록/수정 동일 API로 처리됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "완료 처리 성공",
                    content = @Content(schema = @Schema(implementation = UploadProfileImgResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 key 또는 요청",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(mediaType = "application/json")
            )
    })
    ResponseEntity<UploadProfileImgResponse> completeProfileImage(
            @RequestBody CompleteProfileImageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
