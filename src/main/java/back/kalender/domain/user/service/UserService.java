package back.kalender.domain.user.service;

import back.kalender.domain.user.dto.request.UpdateProfileRequest;
import back.kalender.domain.user.dto.request.UserSignupRequest;
import back.kalender.domain.user.dto.response.PresignProfileImageResponse;
import back.kalender.domain.user.dto.response.UploadProfileImgResponse;
import back.kalender.domain.user.dto.response.UserProfileResponse;
import back.kalender.domain.user.dto.response.UserSignupResponse;
import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.mapper.UserBuilder;
import back.kalender.domain.user.repository.UserRepository;
import back.kalender.global.common.enums.Gender;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3PresignService s3PresignService;

    // 회원가입
    @Transactional
    public UserSignupResponse signup(UserSignupRequest request) {
        // 이메일 중복 확인
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ServiceException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 닉네임 중복 확인
        if (userRepository.findByNickname(request.nickname()).isPresent()) {
            throw new ServiceException(ErrorCode.DUPLICATE_NICKNAME);
        }

        // Gender 변환
        Gender gender = null;
        if (request.gender() != null && !request.gender().isEmpty()) {
            try {
                gender = Gender.valueOf(request.gender().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ServiceException(ErrorCode.BAD_REQUEST);
            }
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // 유저 생성
        User user = UserBuilder.create(request, encodedPassword, gender);
        User savedUser = userRepository.save(user);

        return UserSignupResponse.from(savedUser);
    }

    /**
     * 내 정보 조회
     */
    public UserProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[User] [GetProfile] 유저를 찾을 수 없음 - userId={}", userId);
                    return new ServiceException(ErrorCode.USER_NOT_FOUND);
                });
        String profileImageUrl = s3PresignService.presignProfileImageGet(user.getProfileImage());

        return UserProfileResponse.from(user, profileImageUrl);
    }

    /**
     * presigned PUT 발급
     */
    public PresignProfileImageResponse presignProfileImageUpload(Long userId, String contentType) {
        log.info("[User] [PresignPut] presigned PUT 발급 - userId={}, contentType={}", userId, contentType);

        if (contentType == null || contentType.isBlank()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST);
        }

        return s3PresignService.presignProfileImagePut(userId, contentType);
    }


    /**
     * 업로드 완료 처리: key를 DB에 저장하고, 조회용 presigned GET URL 반환
     */
    @Transactional
    public UploadProfileImgResponse completeProfileImageUpload(Long userId, String key) {

        if (key == null || key.isBlank()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[User] [CompleteUpload] 유저를 찾을 수 없음 - userId={}", userId);
                    return new ServiceException(ErrorCode.USER_NOT_FOUND);
                });

        String expectedPrefix = "profile/" + userId + "/";
        if (!key.startsWith(expectedPrefix)) {
            log.warn("[User] [CompleteUpload] 잘못된 key - userId={}, key={}", userId, key);
            throw new ServiceException(ErrorCode.BAD_REQUEST);
        }

        user.updateProfileImage(key);

        String profileImageUrl = s3PresignService.presignProfileImageGet(key);
        return new UploadProfileImgResponse(profileImageUrl);
    }


    /**
     * 내 정보 수정
     */
    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[User] [UpdateProfile] 유저를 찾을 수 없음 - userId={}", userId);
                    return new ServiceException(ErrorCode.USER_NOT_FOUND);
                });

        // 닉네임 수정
        if (request.nickname() != null) {
            validateNickname(request.nickname(), userId);
            user.updateNickname(request.nickname());
        }

        String profileImageUrl = s3PresignService.presignProfileImageGet(user.getProfileImage());

        return UserProfileResponse.from(user, profileImageUrl);
    }

    /**
     * 닉네임 중복 검증
     */
    private void validateNickname(String nickname, Long userId) {
        userRepository.findByNickname(nickname)
                .ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(userId)) {
                        log.warn("[User] [ValidateNickname] 닉네임 중복 - nickname={}, existingUserId={}, requestUserId={}",
                                nickname, existingUser.getId(), userId);
                        throw new ServiceException(ErrorCode.DUPLICATE_NICKNAME);
                    }
                });
    }
}
