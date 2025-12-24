package back.kalender.domain.user.controller;

import back.kalender.domain.user.dto.request.CompleteProfileImageRequest;
import back.kalender.domain.user.dto.request.UpdateProfileRequest;
import back.kalender.domain.user.dto.request.UserSignupRequest;
import back.kalender.domain.user.dto.response.PresignProfileImageResponse;
import back.kalender.domain.user.dto.response.UploadProfileImgResponse;
import back.kalender.domain.user.dto.response.UserProfileResponse;
import back.kalender.domain.user.dto.response.UserSignupResponse;
import back.kalender.domain.user.service.UserService;
import back.kalender.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController implements UserControllerSpec {

    private final UserService userService;

    @PostMapping
    @Override
    public ResponseEntity<UserSignupResponse> signup(
            @RequestBody UserSignupRequest request
    ) {
        UserSignupResponse response = userService.signup(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Override
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();

        UserProfileResponse response = userService.getMyProfile(userId);
        return ResponseEntity.ok(response);
    }


    @PatchMapping("/me")
    @Override
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails

    ) {
        Long userId = userDetails.getUserId();

        UserProfileResponse response = userService.updateMyProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/profile-image/presign")
    @Override
    public ResponseEntity<PresignProfileImageResponse> presignProfileImage(
            @RequestParam("contentType") String contentType,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        PresignProfileImageResponse response = userService.presignProfileImageUpload(userId, contentType);
        return ResponseEntity.ok(response);
    }

    // 업로드 완료: key 저장 + presigned GET URL 반환
    @PostMapping("/me/profile-image/complete")
    @Override
    public ResponseEntity<UploadProfileImgResponse> completeProfileImage(
            @RequestBody CompleteProfileImageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        UploadProfileImgResponse response = userService.completeProfileImageUpload(userId, request.Key());
        return ResponseEntity.ok(response);
    }

}
