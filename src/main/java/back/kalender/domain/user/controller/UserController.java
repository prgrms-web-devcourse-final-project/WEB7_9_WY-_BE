package back.kalender.domain.user.controller;

import back.kalender.domain.user.dto.request.UpdateProfileRequest;
import back.kalender.domain.user.dto.request.UserSignupRequest;
import back.kalender.domain.user.dto.response.UploadProfileImgResponse;
import back.kalender.domain.user.dto.response.UserProfileResponse;
import back.kalender.domain.user.dto.response.UserSignupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
    // TODO: Service 주입 필요

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
                    description = "잘못된 요청 (이메일 중복, 유효성 검증 실패 등)",
                    content = @Content()
            )
    })
    @PostMapping
    public ResponseEntity<UserSignupResponse> signup(
            @RequestBody UserSignupRequest request
    ) {
        // TODO: userService.signup(request)

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
                    content = @Content()
            )
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        UserProfileResponse dummyData = new UserProfileResponse(
                "user@example.com",
                "닉네임",
                "https://s3.amazonaws.com/kalender/profile/user.jpg",
                4,
                25,
                "MALE"
        );

        return ResponseEntity.ok(dummyData);
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
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content()
            )
    })
    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadProfileImgResponse> uploadProfileImage(
            @RequestParam("profile_image") MultipartFile profileImage
    ) {

        UploadProfileImgResponse dummyData = new UploadProfileImgResponse(
                "https://s3.amazonaws.com/kalender/profile/abc123-profile.jpg"
        );        return ResponseEntity.ok(dummyData);
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
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content()
            )
    })
    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @RequestBody UpdateProfileRequest request
    ) {
        UserProfileResponse dummyData = new UserProfileResponse(
                "user@example.com",
                request.nickname() != null ? request.nickname() : "새로운 닉네임",
                request.profileImage() != null ? request.profileImage() : "https://s3.amazonaws.com/kalender/profile/user.jpg",
                4,
                25,
                "MALE"
        );

        return ResponseEntity.ok(dummyData);
    }
}
