package back.kalender.domain.auth.service;

import back.kalender.domain.auth.dto.request.*;
import back.kalender.domain.auth.dto.response.EmailStatusResponse;
import back.kalender.domain.auth.dto.response.VerifyEmailResponse;
import back.kalender.domain.auth.entity.EmailVerification;
import back.kalender.domain.auth.entity.PasswordResetToken;
import back.kalender.domain.auth.entity.RefreshToken;
import back.kalender.domain.auth.repository.EmailVerificationRepository;
import back.kalender.domain.auth.repository.PasswordResetTokenRepository;
import back.kalender.domain.auth.repository.RefreshTokenRepository;
import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.repository.UserRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import back.kalender.global.security.jwt.JwtProperties;
import back.kalender.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceImplTest {

    private static final String COOKIE_NAME_REFRESH_TOKEN = "refreshToken";
    private static final String HEADER_NAME_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final long ACCESS_TOKEN_EXPIRY_MILLIS = 1800000L;
    private static final long REFRESH_TOKEN_EXPIRY_MILLIS = 1209600000L;
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 14L;
    private static final int PASSWORD_RESET_TOKEN_EXPIRY_MINUTES = 60;
    private static final int EMAIL_VERIFICATION_CODE_EXPIRY_MINUTES = 5;

    private static final String TEST_EMAIL = "tester@test.com";
    private static final String TEST_PASSWORD = "test1234";
    private static final String TEST_NICKNAME = "테스트유저";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final Long TEST_USER_ID = 1L;

    private static final String TEST_VERIFICATION_CODE = "123456";
    private static final String TEST_INVALID_CODE = "999999";
    private static final int VERIFICATION_CODE_LENGTH = 6;

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailVerificationRepository emailVerificationRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private JwtProperties jwtProperties;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private HttpServletResponse httpServletResponse;
    @Mock private JwtProperties.TokenExpiration tokenExpiration;
    @Mock private JwtProperties.CookieProperties cookieProperties;

    @InjectMocks
    private AuthServiceImpl authService;

    
    //---------------------- FIXTURE ------------------------------
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = createTestUser(TEST_EMAIL, ENCODED_PASSWORD, false);
        setField(testUser, "id", TEST_USER_ID);
    }

    
    //---------------------- HELPER -------------------------------
    private User createTestUser(String email, String password, boolean emailVerified) {
        return User.builder()
                .email(email)
                .password(password)
                .nickname(TEST_NICKNAME)
                .emailVerified(emailVerified)
                .build();
    }

    private RefreshToken createRefreshToken(Long userId, String token) {
        return RefreshToken.create(userId, token, REFRESH_TOKEN_EXPIRY_DAYS);
    }

    private PasswordResetToken createPasswordResetToken(Long userId, String token) {
        return PasswordResetToken.create(userId, token, PASSWORD_RESET_TOKEN_EXPIRY_MINUTES);
    }

    private EmailVerification createEmailVerification(Long userId, String code) {
        return EmailVerification.create(userId, code, EMAIL_VERIFICATION_CODE_EXPIRY_MINUTES);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> clazz = target.getClass();
            Field field = null;
            
            while (clazz != null && field == null) {
                try {
                    field = clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            
            if (field == null) {
                throw new NoSuchFieldException("Field not found: " + fieldName);
            }
            
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName + " on " + target.getClass().getSimpleName(), e);
        }
    }

    private void setupJwtProperties() {
        given(jwtProperties.getTokenExpiration()).willReturn(tokenExpiration);
        given(tokenExpiration.getAccessInMillis()).willReturn(ACCESS_TOKEN_EXPIRY_MILLIS);
        given(tokenExpiration.getRefreshInMillis()).willReturn(REFRESH_TOKEN_EXPIRY_MILLIS);
        given(tokenExpiration.getRefresh()).willReturn(REFRESH_TOKEN_EXPIRY_DAYS);
        setupCookieProperties();
    }

    private void setupCookieProperties() {
        given(jwtProperties.getCookie()).willReturn(cookieProperties);
        given(cookieProperties.isSecure()).willReturn(false);
    }

    private void givenUserExists() {
        given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
    }

    private void givenUserById() {
        given(userRepository.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
    }

    private void givenTokenCreation(String accessToken, String refreshToken) {
        given(jwtTokenProvider.createToken(String.valueOf(TEST_USER_ID), ACCESS_TOKEN_EXPIRY_MILLIS))
                .willReturn(accessToken);
        given(jwtTokenProvider.createToken(String.valueOf(TEST_USER_ID), REFRESH_TOKEN_EXPIRY_MILLIS))
                .willReturn(refreshToken);
    }

    private Cookie captureCookie() {
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(httpServletResponse, times(1)).addCookie(cookieCaptor.capture());
        return cookieCaptor.getValue();
    }

    private void assertRefreshTokenCookie(String expectedToken) {
        Cookie cookie = captureCookie();
        assertThat(cookie.getName()).isEqualTo(COOKIE_NAME_REFRESH_TOKEN);
        assertThat(cookie.getValue()).isEqualTo(expectedToken);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isFalse();
    }

    private void assertLogoutCookie() {
        Cookie cookie = captureCookie();
        assertThat(cookie.getName()).isEqualTo(COOKIE_NAME_REFRESH_TOKEN);
        assertThat(cookie.getValue()).isIn("", null);
        assertThat(cookie.getMaxAge()).isEqualTo(0);
        assertThat(cookie.getSecure()).isFalse();
    }

    private void captureAndAssertVerificationCode() {
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1)).sendVerificationEmail(eq(TEST_EMAIL), codeCaptor.capture());
        String code = codeCaptor.getValue();
        assertThat(code).isNotNull();
        assertThat(code)
                .as("인증 코드는 6자리 숫자여야 함")
                .hasSize(VERIFICATION_CODE_LENGTH)
                .matches("^\\d{" + VERIFICATION_CODE_LENGTH + "}$");
    }

    private void captureAndAssertPasswordResetToken() {
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1)).sendPasswordResetEmail(eq(TEST_EMAIL), tokenCaptor.capture());
        String token = tokenCaptor.getValue();
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    private RefreshToken captureRefreshToken() {
        ArgumentCaptor<RefreshToken> rtCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(1)).save(rtCaptor.capture());
        return rtCaptor.getValue();
    }

    //---------------------- TEST CODE ----------------------------
    @Nested
    @DisplayName("login 테스트")
    class LoginTest {

        @Test
        @DisplayName("로그인 성공")
        void login_Success() {
            UserLoginRequest request = new UserLoginRequest(TEST_EMAIL, TEST_PASSWORD);
            String accessToken = "accessToken";
            String refreshToken = "refreshToken";

            givenUserExists();
            given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
            setupJwtProperties();
            givenTokenCreation(accessToken, refreshToken);

            authService.login(request, httpServletResponse);

            assertRefreshTokenCookie(refreshToken);
        }

        @Test
        @DisplayName("로그인 실패 - 유저 없음")
        void login_Fail_UserNotFound() {
            String notFoundEmail = "notfound@test.com";
            UserLoginRequest request = new UserLoginRequest(notFoundEmail, TEST_PASSWORD);

            given(userRepository.findByEmail(notFoundEmail)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request, httpServletResponse))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

            verify(userRepository).findByEmail(notFoundEmail);
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("로그인 실패 - 비밀번호 불일치")
        void login_Fail_PasswordMismatch() {
            String wrongPassword = "wrongPassword";
            UserLoginRequest request = new UserLoginRequest(TEST_EMAIL, wrongPassword);

            givenUserExists();
            given(passwordEncoder.matches(wrongPassword, ENCODED_PASSWORD)).willReturn(false);

            assertThatThrownBy(() -> authService.login(request, httpServletResponse))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(passwordEncoder).matches(wrongPassword, ENCODED_PASSWORD);
            verify(jwtTokenProvider, never()).createToken(anyString(), anyLong());
        }
    }

    @Nested
    @DisplayName("logout 테스트")
    class LogoutTest {

        @Test
        @DisplayName("로그아웃 성공")
        void logout_Success() {
            String refreshToken = "refreshToken";
            RefreshToken refreshTokenEntity = createRefreshToken(TEST_USER_ID, refreshToken);

            given(refreshTokenRepository.findByToken(refreshToken)).willReturn(Optional.of(refreshTokenEntity));
            setupJwtProperties();

            authService.logout(refreshToken, httpServletResponse);

            assertLogoutCookie();
        }
    }

    @Nested
    @DisplayName("refreshToken 테스트")
    class RefreshTokenTest {

        @Test
        @DisplayName("토큰 갱신 성공")
        void refreshToken_Success() {
            String oldRefreshToken = "oldRefreshToken";
            RefreshToken oldEntity = createRefreshToken(TEST_USER_ID, oldRefreshToken);
            String newAccessToken = "newAccessToken";
            String newRefreshToken = "newRefreshToken";

            given(jwtTokenProvider.validateToken(oldRefreshToken)).willReturn(true);
            given(refreshTokenRepository.findByToken(oldRefreshToken)).willReturn(Optional.of(oldEntity));
            givenUserById();
            setupJwtProperties();
            givenTokenCreation(newAccessToken, newRefreshToken);

            authService.refreshToken(oldRefreshToken, httpServletResponse);

            RefreshToken saved = captureRefreshToken();
            assertThat(saved.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(saved.getToken()).isEqualTo(newRefreshToken);

            verify(refreshTokenRepository).delete(oldEntity);
            verify(httpServletResponse).setHeader(HEADER_NAME_AUTHORIZATION, BEARER_PREFIX + newAccessToken);
        }

        @Test
        @DisplayName("토큰 갱신 실패 - 토큰 없음")
        void refreshToken_Fail_TokenNull() {
            assertThatThrownBy(() -> authService.refreshToken(null, httpServletResponse))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

            verify(jwtTokenProvider, never()).validateToken(anyString());
        }
    }

    @Nested
    @DisplayName("sendPasswordResetEmail 테스트")
    class SendPasswordResetEmailTest {

        @Test
        @DisplayName("비밀번호 재설정 이메일 발송 성공")
        void sendPasswordResetEmail_Success() {
            UserPasswordResetSendRequest request = new UserPasswordResetSendRequest(TEST_EMAIL);

            givenUserExists();

            authService.sendPasswordResetEmail(request);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(passwordResetTokenRepository).deleteByUserId(TEST_USER_ID);
            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
            captureAndAssertPasswordResetToken();
        }
    }

    @Nested
    @DisplayName("resetPassword 테스트")
    class ResetPasswordTest {

        @Test
        @DisplayName("비밀번호 재설정 성공")
        void resetPassword_Success() {
            String token = "reset-token-12345";
            String newPassword = "newPassword123";
            String encodedNewPassword = "encodedNewPassword";
            PasswordResetToken resetToken = createPasswordResetToken(TEST_USER_ID, token);

            UserPasswordResetRequest request = new UserPasswordResetRequest(
                    token, newPassword, newPassword
            );

            given(passwordResetTokenRepository.findByToken(token)).willReturn(Optional.of(resetToken));
            givenUserById();
            given(passwordEncoder.encode(newPassword)).willReturn(encodedNewPassword);

            authService.resetPassword(request);

            assertThat(testUser.getPassword()).isEqualTo(encodedNewPassword);
            assertThat(resetToken.isUsed()).isTrue();
            verify(passwordResetTokenRepository).findByToken(token);
            verify(userRepository).findById(TEST_USER_ID);
            verify(passwordEncoder).encode(newPassword);
            verify(passwordResetTokenRepository).save(resetToken);
        }

        @Test
        @DisplayName("비밀번호 재설정 실패 - 비밀번호 불일치")
        void resetPassword_Fail_PasswordMismatch() {
            String token = "reset-token-12345";
            UserPasswordResetRequest request = new UserPasswordResetRequest(
                    token, "newPassword123", "differentPassword"
            );

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PASSWORD_MISMATCH);

            verify(passwordResetTokenRepository, never()).findByToken(anyString());
        }
    }

    @Nested
    @DisplayName("sendVerifyEmail 테스트")
    class SendVerifyEmailTest {

        @Test
        @DisplayName("이메일 인증 코드 발송 성공")
        void sendVerifyEmail_Success() {
            VerifyEmailSendRequest request = new VerifyEmailSendRequest(TEST_EMAIL);

            givenUserExists();
            given(emailVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(TEST_USER_ID))
                    .willReturn(Optional.empty());

            authService.sendVerifyEmail(request);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(emailVerificationRepository).deleteByUserId(TEST_USER_ID);
            verify(emailVerificationRepository).save(any(EmailVerification.class));
            captureAndAssertVerificationCode();
        }

        @Test
        @DisplayName("이메일 인증 코드 발송 실패 - 이미 인증됨")
        void sendVerifyEmail_Fail_AlreadyVerified() {
            testUser.verifyEmail();
            VerifyEmailSendRequest request = new VerifyEmailSendRequest(TEST_EMAIL);

            givenUserExists();

            assertThatThrownBy(() -> authService.sendVerifyEmail(request))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMAIL_ALREADY_VERIFIED);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("verifyEmail 테스트")
    class VerifyEmailTest {

        @Test
        @DisplayName("이메일 인증 성공")
        void verifyEmail_Success() {
            EmailVerification verification = createEmailVerification(TEST_USER_ID, TEST_VERIFICATION_CODE);
            VerifyEmailRequest request = new VerifyEmailRequest(TEST_EMAIL, TEST_VERIFICATION_CODE);

            givenUserExists();
            given(emailVerificationRepository.findByUserIdAndCode(TEST_USER_ID, TEST_VERIFICATION_CODE))
                    .willReturn(Optional.of(verification));

            VerifyEmailResponse response = authService.verifyEmail(request);

            assertThat(response.email()).isEqualTo(TEST_EMAIL);
            assertThat(response.emailVerified()).isTrue();
            assertThat(testUser.isEmailVerified()).isTrue();
            assertThat(verification.isUsed()).isTrue();

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(emailVerificationRepository).findByUserIdAndCode(TEST_USER_ID, TEST_VERIFICATION_CODE);
            verify(emailVerificationRepository).save(verification);
        }

        @Test
        @DisplayName("이메일 인증 실패 - 코드 없음")
        void verifyEmail_Fail_CodeNotFound() {
            VerifyEmailRequest request = new VerifyEmailRequest(TEST_EMAIL, TEST_INVALID_CODE);

            givenUserExists();
            given(emailVerificationRepository.findByUserIdAndCode(TEST_USER_ID, TEST_INVALID_CODE))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmail(request))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMAIL_VERIFICATION_CODE_NOT_FOUND);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(emailVerificationRepository).findByUserIdAndCode(TEST_USER_ID, TEST_INVALID_CODE);
        }
    }

    @Nested
    @DisplayName("getEmailStatus 테스트")
    class GetEmailStatusTest {

        @Test
        @DisplayName("이메일 상태 조회 성공")
        void getEmailStatus_Success() {
            LocalDateTime verifiedAt = LocalDateTime.now().minusDays(1);
            EmailVerification verification = createEmailVerification(TEST_USER_ID, TEST_VERIFICATION_CODE);
            verification.markUsed();
            setField(verification, "updatedAt", verifiedAt);

            testUser.verifyEmail();
            givenUserById();
            given(emailVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(TEST_USER_ID))
                    .willReturn(Optional.of(verification));

            EmailStatusResponse response = authService.getEmailStatus(TEST_USER_ID);

            assertThat(response.userId()).isEqualTo(TEST_USER_ID);
            assertThat(response.email()).isEqualTo(TEST_EMAIL);
            assertThat(response.emailVerified()).isTrue();
            assertThat(response.verifiedAt()).isEqualTo(verifiedAt);

            verify(userRepository).findById(TEST_USER_ID);
            verify(emailVerificationRepository).findTopByUserIdOrderByCreatedAtDesc(TEST_USER_ID);
        }
    }
}
