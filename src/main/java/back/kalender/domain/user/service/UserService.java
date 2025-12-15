package back.kalender.domain.user.service;


import back.kalender.domain.user.dto.request.UpdateProfileRequest;
import back.kalender.domain.user.dto.request.UserSignupRequest;
import back.kalender.domain.user.dto.response.UploadProfileImgResponse;
import back.kalender.domain.user.dto.response.UserProfileResponse;
import back.kalender.domain.user.dto.response.UserSignupResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserSignupResponse signup(UserSignupRequest request);
    UserProfileResponse getMyProfile(Long userId);
    UploadProfileImgResponse uploadProfileImage(Long userId, MultipartFile profileImage);
    UserProfileResponse updateMyProfile(Long userId, UpdateProfileRequest request);
}