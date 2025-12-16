package back.kalender.domain.user.service;

import back.kalender.domain.user.dto.request.UpdateProfileRequest;
import back.kalender.domain.user.dto.request.UserSignupRequest;
import back.kalender.domain.user.dto.response.UploadProfileImgResponse;
import back.kalender.domain.user.dto.response.UserProfileResponse;
import back.kalender.domain.user.dto.response.UserSignupResponse;
import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.mapper.UserBuilder;
import back.kalender.domain.user.repository.UserRepository;
import back.kalender.global.common.Enum.Gender;
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
        log.debug("[User] [GetProfile] 프로필 조회 시작 - userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[User] [GetProfile] 유저를 찾을 수 없음 - userId={}", userId);
                    return new ServiceException(ErrorCode.USER_NOT_FOUND);
                });

        log.debug("[User] [GetProfile] 프로필 조회 완료 - userId={}, nickname={}", userId, user.getNickname());
        return UserProfileResponse.from(user);
    }

    /**
     * 프로필 이미지 업로드
     */
    @Transactional
    public UploadProfileImgResponse uploadProfileImage(Long userId, MultipartFile profileImage) {
        log.info("[User] [UploadImage] 이미지 업로드 시작 - userId={}, fileName={}",
                userId, profileImage.getOriginalFilename());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[User] [UploadImage] 유저를 찾을 수 없음 - userId={}", userId);
                    return new ServiceException(ErrorCode.USER_NOT_FOUND);
                });

        // TODO: S3에 이미지 업로드
        // String imageUrl = s3Service.uploadFile(profileImage, "profile");

        // 임시 URL (실제로는 S3 업로드 후 받은 URL 사용)
        String imageUrl = "https://s3.amazonaws.com/kalender/profile/" + userId + ".jpg";
        log.debug("[User] [UploadImage] S3 업로드 완료 (임시) - userId={}, imageUrl={}", userId, imageUrl);

        user.updateProfileImage(imageUrl);
        log.info("[User] [UploadImage] 이미지 업로드 완료 - userId={}, imageUrl={}", userId, imageUrl);

        return new UploadProfileImgResponse(imageUrl);
    }

    /**
     * 내 정보 수정
     */
    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        log.info("[User] [UpdateProfile] 프로필 수정 시작 - userId={}, nickname={}",
                userId, request.nickname());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[User] [UpdateProfile] 유저를 찾을 수 없음 - userId={}", userId);
                    return new ServiceException(ErrorCode.USER_NOT_FOUND);
                });

        // 닉네임 수정
        if (request.nickname() != null) {
            log.debug("[User] [UpdateProfile] 닉네임 변경 시도 - userId={}, oldNickname={}, newNickname={}",
                    userId, user.getNickname(), request.nickname());
            validateNickname(request.nickname(), userId);
            user.updateNickname(request.nickname());
        }

        // 프로필 이미지 수정
        if (request.profileImage() != null) {
            log.debug("[User] [UpdateProfile] 프로필 이미지 변경 - userId={}, imageUrl={}",
                    userId, request.profileImage());
            user.updateProfileImage(request.profileImage());
        }
        log.info("[User] [UpdateProfile] 프로필 수정 완료 - userId={}", userId);

        return UserProfileResponse.from(user);
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
