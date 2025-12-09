package back.kalender.domain.user;

import back.kalender.domain.user.dto.request.UpdateProfileRequest;
import back.kalender.domain.user.dto.response.UploadProfileImgResponse;
import back.kalender.domain.user.dto.response.UserProfileResponse;
import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.repository.UserRepository;
import back.kalender.domain.user.service.UserServiceImpl;
import back.kalender.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
public class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("내 정보 조회 - 성공")
    void getMyProfile_Success() {
        Long userId = 1L;
        User mockUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .gender("F")
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        UserProfileResponse response = userService.getMyProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.nickname()).isEqualTo("테스트유저");
        assertThat(response.gender()).isEqualTo("F");
        assertThat(response.age()).isEqualTo(25); // 2025 - 2000 = 25

        verify(userRepository, times(1)).findById(userId);
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
    }

    @Test
    @DisplayName("프로필 이미지 업로드 - 성공")
    void uploadProfileImage_Success() {
        Long userId = 1L;
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        User mockUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .gender("F")
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        UploadProfileImgResponse response = userService.uploadProfileImage(userId, mockFile);

        assertThat(response).isNotNull();
        assertThat(response.profileImageUrl()).contains("https://s3.amazonaws.com/kalender/profile/");

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("프로필 이미지 업로드 - 유저를 찾을 수 없음")
    void uploadProfileImage_UserNotFound() {
        Long userId = 999L;
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.uploadProfileImage(userId, mockFile))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("유저를 찾을 수 없습니다");

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("내 정보 수정 - 닉네임만 수정 성공")
    void updateMyProfile_OnlyNickname_Success() {
        Long userId = 1L;
        UpdateProfileRequest request = new UpdateProfileRequest("새닉네임", null);

        User mockUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("기존닉네임")
                .gender("F")
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(userRepository.findByNickname("새닉네임")).thenReturn(Optional.empty());

        UserProfileResponse response = userService.updateMyProfile(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.nickname()).isEqualTo("새닉네임");

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).findByNickname("새닉네임");
    }

    @Test
    @DisplayName("내 정보 수정 - 프로필 이미지만 수정 성공")
    void updateMyProfile_OnlyProfileImage_Success() {
        Long userId = 1L;
        String newImageUrl = "https://new-image-url.com/profile.jpg";
        UpdateProfileRequest request = new UpdateProfileRequest(null, newImageUrl);

        User mockUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .profileImage("https://old-image-url.com/profile.jpg")
                .gender("F")
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        UserProfileResponse response = userService.updateMyProfile(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.profileImage()).isEqualTo(newImageUrl);

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).findByNickname(any());
    }

    @Test
    @DisplayName("내 정보 수정 - 닉네임과 프로필 이미지 모두 수정 성공")
    void updateMyProfile_BothFields_Success() {
        Long userId = 1L;
        String newNickname = "새닉네임";
        String newImageUrl = "https://new-image-url.com/profile.jpg";
        UpdateProfileRequest request = new UpdateProfileRequest(newNickname, newImageUrl);

        User mockUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("기존닉네임")
                .profileImage("https://old-image-url.com/profile.jpg")
                .gender("F")
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(userRepository.findByNickname(newNickname)).thenReturn(Optional.empty());

        UserProfileResponse response = userService.updateMyProfile(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.nickname()).isEqualTo(newNickname);
        assertThat(response.profileImage()).isEqualTo(newImageUrl);

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).findByNickname(newNickname);
    }

    @Test
    @DisplayName("내 정보 수정 - 닉네임 중복으로 실패")
    void updateMyProfile_DuplicateNickname_Fail() {
        Long userId = 1L;
        UpdateProfileRequest request = new UpdateProfileRequest("중복닉네임", null);

        User currentUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("기존닉네임")
                .gender("F")
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();
        setUserId(currentUser, 1L);

        User otherUser = User.builder()
                .email("other@example.com")
                .password("password123")
                .nickname("중복닉네임")
                .gender("M")
                .birthDate(LocalDate.of(1995, 5, 15))
                .build();
        setUserId(otherUser, 2L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByNickname("중복닉네임")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> userService.updateMyProfile(userId, request))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("이미 사용 중인 닉네임입니다");

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).findByNickname("중복닉네임");
    }

    @Test
    @DisplayName("내 정보 수정 - 유저를 찾을 수 없음")
    void updateMyProfile_UserNotFound() {
        Long userId = 999L;
        UpdateProfileRequest request = new UpdateProfileRequest("새닉네임", null);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateMyProfile(userId, request))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("유저를 찾을 수 없습니다");

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).findByNickname(any());
    }

    private void setUserId(User user, Long id) {
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException("UserId 설정 실패", e);
        }
    }
}
