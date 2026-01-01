package back.kalender.domain.user.service;

import back.kalender.domain.user.dto.request.UpdateProfileRequest;
import back.kalender.domain.user.dto.response.PresignProfileImageResponse;
import back.kalender.domain.user.dto.response.UploadProfileImgResponse;
import back.kalender.domain.user.dto.response.UserProfileResponse;
import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.repository.UserRepository;
import back.kalender.global.common.enums.Gender;
import back.kalender.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private S3PresignService s3PresignService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("내 정보 조회 - 성공")
    void getMyProfile_Success() {
        Long userId = 1L;
        User mockUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .profileImage("profile/1/uuid")
                .gender(Gender.FEMALE)
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(s3PresignService.presignProfileImageGet("profile/1/uuid"))
                .thenReturn("https://presigned-get-url");

        UserProfileResponse response = userService.getMyProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.nickname()).isEqualTo("테스트유저");
        assertThat(response.gender()).isEqualTo(Gender.FEMALE);
        assertThat(response.age()).isEqualTo(26); // 2025 - 2000 = 25
        assertThat(response.profileImage()).isEqualTo("https://presigned-get-url");

        verify(userRepository, times(1)).findById(userId);
        verify(s3PresignService, times(1)).presignProfileImageGet("profile/1/uuid");
    }

    @Test
    @DisplayName("내 정보 조회 - 프로필 이미지가 null이면 profileImageUrl도 null")
    void getMyProfile_ProfileImageNull() {
        Long userId = 1L;
        User mockUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .profileImage(null)
                .gender(Gender.FEMALE)
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(s3PresignService.presignProfileImageGet(null)).thenReturn(null);

        UserProfileResponse response = userService.getMyProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.profileImage()).isNull();

        verify(userRepository, times(1)).findById(userId);
        verify(s3PresignService, times(1)).presignProfileImageGet(null);
    }

    @Test
    @DisplayName("내 정보 조회 - 유저를 찾을 수 없음")
    void getMyProfile_UserNotFound() {
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile(userId))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("유저를 찾을 수 없습니다");

        verify(userRepository, times(1)).findById(userId);
        verifyNoInteractions(s3PresignService);
    }

    @Test
    @DisplayName("프로필 이미지 presign PUT 발급 - 성공")
    void presignProfileImageUpload_Success() {
        Long userId = 1L;
        String contentType = "image/jpeg";

        PresignProfileImageResponse mockResponse =
                new PresignProfileImageResponse(
                        "profile/1/uuid",
                        "https://presigned-put-url",
                        Map.of("Content-Type", contentType),
                        600
                );

        when(s3PresignService.presignProfileImagePut(userId, contentType))
                .thenReturn(mockResponse);

        PresignProfileImageResponse response = userService.presignProfileImageUpload(userId, contentType);

        assertThat(response).isNotNull();
        assertThat(response.key()).isEqualTo("profile/1/uuid");
        assertThat(response.uploadUrl()).isEqualTo("https://presigned-put-url");
        assertThat(response.requiredHeaders()).containsEntry("Content-Type", contentType);

        verify(s3PresignService, times(1)).presignProfileImagePut(userId, contentType);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("프로필 이미지 presign PUT 발급 - contentType이 비어있으면 실패")
    void presignProfileImageUpload_BadRequest() {
        Long userId = 1L;

        assertThatThrownBy(() -> userService.presignProfileImageUpload(userId, ""))
                .isInstanceOf(ServiceException.class);

        verifyNoInteractions(s3PresignService);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("프로필 이미지 업로드 완료(complete) - 성공 (key 저장 + presigned GET URL 반환)")
    void completeProfileImageUpload_Success() {
        Long userId = 1L;

        User mockUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .profileImage(null)
                .gender(Gender.FEMALE)
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();
        setUserId(mockUser, userId);

        String key = "profile/1/uuid";
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(s3PresignService.presignProfileImageGet(key)).thenReturn("https://presigned-get-url");

        UploadProfileImgResponse response = userService.completeProfileImageUpload(userId, key);

        assertThat(response).isNotNull();
        assertThat(response.profileImageUrl()).isEqualTo("https://presigned-get-url");
        assertThat(mockUser.getProfileImage()).isEqualTo(key); // ✅ key 저장 확인

        verify(userRepository, times(1)).findById(userId);
        verify(s3PresignService, times(1)).presignProfileImageGet(key);
    }

    @Test
    @DisplayName("프로필 이미지 업로드 완료(complete) - key prefix가 profile/{userId}/가 아니면 실패")
    void completeProfileImageUpload_InvalidKeyPrefix_Fail() {
        Long userId = 1L;

        User mockUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .build();
        setUserId(mockUser, userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        String invalidKey = "profile/999/uuid"; // userId mismatch

        assertThatThrownBy(() -> userService.completeProfileImageUpload(userId, invalidKey))
                .isInstanceOf(ServiceException.class);

        verify(userRepository, times(1)).findById(userId);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(s3PresignService);
    }

    @Test
    @DisplayName("프로필 이미지 업로드 완료(complete) - 유저를 찾을 수 없음")
    void completeProfileImageUpload_UserNotFound() {
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.completeProfileImageUpload(userId, "profile/999/uuid"))
                .isInstanceOf(ServiceException.class);

        verify(userRepository, times(1)).findById(userId);
        verifyNoInteractions(s3PresignService);
    }

    @Test
    @DisplayName("내 정보 수정 - 닉네임만 수정 성공")
    void updateMyProfile_OnlyNickname_Success() {
        Long userId = 1L;
        UpdateProfileRequest request = new UpdateProfileRequest("새닉네임");

        User mockUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("기존닉네임")
                .profileImage("profile/1/old")
                .gender(Gender.FEMALE)
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();
        setUserId(mockUser, userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(userRepository.findByNickname("새닉네임")).thenReturn(Optional.empty());
        when(s3PresignService.presignProfileImageGet("profile/1/old"))
                .thenReturn("https://presigned-get-url-old");

        UserProfileResponse response = userService.updateMyProfile(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.nickname()).isEqualTo("새닉네임");
        assertThat(response.profileImage()).isEqualTo("https://presigned-get-url-old");

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).findByNickname("새닉네임");
        verify(s3PresignService, times(1)).presignProfileImageGet("profile/1/old");
    }

    @Test
    @DisplayName("내 정보 수정 - 닉네임 중복으로 실패")
    void updateMyProfile_DuplicateNickname_Fail() {
        Long userId = 1L;
        UpdateProfileRequest request = new UpdateProfileRequest("중복닉네임");

        User currentUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("기존닉네임")
                .build();
        setUserId(currentUser, 1L);

        User otherUser = User.builder()
                .email("other@example.com")
                .password("password123")
                .nickname("중복닉네임")
                .build();
        setUserId(otherUser, 2L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByNickname("중복닉네임")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> userService.updateMyProfile(userId, request))
                .isInstanceOf(ServiceException.class);

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).findByNickname("중복닉네임");
        verifyNoInteractions(s3PresignService);
    }

    @Test
    @DisplayName("내 정보 수정 - 유저를 찾을 수 없음")
    void updateMyProfile_UserNotFound() {
        Long userId = 999L;
        UpdateProfileRequest request = new UpdateProfileRequest("새닉네임");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateMyProfile(userId, request))
                .isInstanceOf(ServiceException.class);

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).findByNickname(any());
        verifyNoInteractions(s3PresignService);
    }

    private void setUserId(User user, Long id) {
        try {
            Field idField = user.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException("UserId 설정 실패", e);
        }
    }
}
