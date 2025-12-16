package back.kalender.domain.user.controller;

import back.kalender.domain.user.dto.request.UpdateProfileRequest;
import back.kalender.domain.user.dto.request.UserSignupRequest;
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


    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ResponseEntity<UploadProfileImgResponse> uploadProfileImage(
            @RequestParam("profile_image") MultipartFile profileImage,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();

        UploadProfileImgResponse response = userService.uploadProfileImage(userId, profileImage);
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
}
