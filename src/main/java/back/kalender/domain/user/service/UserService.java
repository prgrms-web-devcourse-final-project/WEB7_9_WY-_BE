package back.kalender.domain.user.service;


import back.kalender.domain.user.dto.request.UpdateProfileRequest;
import back.kalender.domain.user.dto.response.UploadProfileImgResponse;
import back.kalender.domain.user.dto.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserProfileResponse getMyProfile(Long userId);
    UploadProfileImgResponse uploadProfileImage(Long userId, MultipartFile profileImage);
    UserProfileResponse updateMyProfile(Long userId, UpdateProfileRequest request);
}