package back.kalender.domain.user.service;

import back.kalender.domain.user.dto.request.UpdateProfileRequest;
import back.kalender.domain.user.dto.response.UploadProfileImgResponse;
import back.kalender.domain.user.dto.response.UserProfileResponse;
import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.repository.UserRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService{
    private final UserRepository userRepository;

    /**
     * 내 정보 조회
     */
    @Override
    public UserProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        return UserProfileResponse.from(user);
    }

    /**
     * 프로필 이미지 업로드
     */
    @Override
    @Transactional
    public UploadProfileImgResponse uploadProfileImage(Long userId, MultipartFile profileImage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        // TODO: S3에 이미지 업로드
        // String imageUrl = s3Service.uploadFile(profileImage, "profile");

        // 임시 URL (실제로는 S3 업로드 후 받은 URL 사용)
        String imageUrl = "https://s3.amazonaws.com/kalender/profile/" + userId + ".jpg";

        user.updateProfileImage(imageUrl);

        return new UploadProfileImgResponse(imageUrl);
    }

    /**
     * 내 정보 수정
     */
    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        // 닉네임 수정
        if (request.nickname() != null) {
            validateNickname(request.nickname(), userId);
            user.updateNickname(request.nickname());
        }

        // 프로필 이미지 수정
        if (request.profileImage() != null) {
            user.updateProfileImage(request.profileImage());
        }

        return UserProfileResponse.from(user);
    }

    /**
     * 닉네임 중복 검증
     */
    private void validateNickname(String nickname, Long userId) {
        userRepository.findByNickname(nickname)
                .ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(userId)) {
                        throw new ServiceException(ErrorCode.DUPLICATE_NICKNAME);
                    }
                });
    }
}
