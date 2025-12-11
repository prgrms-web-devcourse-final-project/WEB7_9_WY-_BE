package back.kalender.domain.auth.service;

import back.kalender.domain.auth.dto.request.*;
import back.kalender.domain.auth.dto.response.EmailStatusResponse;
import back.kalender.domain.auth.dto.response.UserLoginResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserLoginResponse login(UserLoginRequest request, HttpServletResponse response) {
        // 유저 조회
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ServiceException(ErrorCode.INVALID_CREDENTIALS));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ServiceException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 토큰 생성
        String accessToken = createAccessToken(user);
        String refreshToken = createRefreshToken(user);

        // Refresh Token DB 저장
        RefreshToken refreshTokenEntity = RefreshToken.create(
                user.getId(),
                refreshToken,
                jwtProperties.getTokenExpiration().getRefresh()
        );
        refreshTokenRepository.save(refreshTokenEntity);

        // Access Token을 Response Header에 설정
        applyAccessTokenHeader(response, accessToken);

        // Refresh Token을 httpOnly secure 쿠키로 설정
        response.addCookie(buildRefreshTokenCookie(refreshToken));

        return new UserLoginResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                user.getProfileImage(),
                user.getEmailVerified() != null ? user.getEmailVerified() : false
        );
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenRepository.findByToken(refreshToken)
                    .ifPresent(refreshTokenRepository::delete);
        }
    }

    @Override
    @Transactional
    public void refreshToken(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new ServiceException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // DB에서 Refresh Token 확인
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new ServiceException(ErrorCode.INVALID_REFRESH_TOKEN));

        // 만료 확인
        if (isExpired(refreshTokenEntity.getExpiredAt())) {
            refreshTokenRepository.delete(refreshTokenEntity);
            throw new ServiceException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        // 유저 조회
        User user = userRepository.findById(refreshTokenEntity.getUserId())
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        // 새 토큰 생성
        String newAccessToken = createAccessToken(user);
        String newRefreshToken = createRefreshToken(user);

        // 기존 Refresh Token 삭제
        refreshTokenRepository.delete(refreshTokenEntity);

        // 새 Refresh Token 저장
        RefreshToken newRefreshTokenEntity = RefreshToken.create(
                user.getId(),
                newRefreshToken,
                jwtProperties.getTokenExpiration().getRefresh()
        );
        refreshTokenRepository.save(newRefreshTokenEntity);

        // Access Token을 Response Header에 설정
        applyAccessTokenHeader(response, newAccessToken);

        // Refresh Token을 httpOnly secure 쿠키로 설정
        response.addCookie(buildRefreshTokenCookie(newRefreshToken));
    }

    @Override
    @Transactional
    public void sendPasswordResetEmail(UserPasswordResetSendRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        // 기존 토큰 삭제
        passwordResetTokenRepository.deleteByUserId(user.getId());

        // 새 토큰 생성
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.create(user.getId(), token);
        passwordResetTokenRepository.save(resetToken);

        // TODO: 이메일 발송 로직 구현
        // emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Override
    @Transactional
    public void resetPassword(UserPasswordResetRequest request) {
        // 비밀번호 일치 확인
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new ServiceException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 토큰 조회
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new ServiceException(ErrorCode.PASSWORD_RESET_TOKEN_NOT_FOUND));

        // 사용 여부 확인
        if (resetToken.isUsed()) {
            throw new ServiceException(ErrorCode.PASSWORD_RESET_TOKEN_ALREADY_USED);
        }

        // 만료 확인
        if (isExpired(resetToken.getExpiredAt())) {
            throw new ServiceException(ErrorCode.EXPIRED_PASSWORD_RESET_TOKEN);
        }

        // 유저 조회 및 비밀번호 업데이트
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        user.updatePassword(passwordEncoder.encode(request.newPassword()));

        // 토큰 사용 처리
        resetToken.markUsed();
    }

    @Override
    @Transactional
    public void sendVerifyEmail(VerifyEmailSendRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        // 이미 인증된 경우
        if (user.getEmailVerified() != null && user.getEmailVerified()) {
            throw new ServiceException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        // 최근 5분 이내 발송된 인증 코드 확인 (재발송 제한)
        emailVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .ifPresent(verification -> {
                    if (verification.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(5))) {
                        throw new ServiceException(ErrorCode.EMAIL_VERIFICATION_LIMIT_EXCEEDED);
                    }
                });

        // 인증 코드 생성 (6자리 숫자)
        String code = String.format("%06d", (int) (Math.random() * 1000000));

        // 기존 인증 코드 삭제
        emailVerificationRepository.deleteByUserId(user.getId());

        // 새 인증 코드 저장
        EmailVerification verification = EmailVerification.create(user.getId(), code);
        emailVerificationRepository.save(verification);

        // TODO: 이메일 발송 로직 구현
        // emailService.sendVerificationEmail(user.getEmail(), code);
    }

    @Override
    @Transactional
    public VerifyEmailResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        // 인증 코드 조회
        EmailVerification verification = emailVerificationRepository.findByCode(request.code())
                .orElseThrow(() -> new ServiceException(ErrorCode.EMAIL_VERIFICATION_CODE_NOT_FOUND));

        // 유저 ID 일치 확인
        if (!verification.getUserId().equals(user.getId())) {
            throw new ServiceException(ErrorCode.INVALID_EMAIL_VERIFICATION_CODE);
        }

        // 사용 여부 확인
        if (verification.isUsed()) {
            throw new ServiceException(ErrorCode.INVALID_EMAIL_VERIFICATION_CODE);
        }

        // 만료 확인
        if (isExpired(verification.getExpiredAt())) {
            throw new ServiceException(ErrorCode.EXPIRED_EMAIL_VERIFICATION_CODE);
        }

        // 인증 처리
        verification.markUsed();
        user.verifyEmail();

        return new VerifyEmailResponse(request.email(), true);
    }

    @Override
    public EmailStatusResponse getEmailStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        // 이메일 인증 시간 조회
        LocalDateTime verifiedAt = emailVerificationRepository
                .findTopByUserIdOrderByCreatedAtDesc(userId)
                .filter(EmailVerification::isUsed)
                .map(EmailVerification::getUpdatedAt)
                .orElse(null);

        return new EmailStatusResponse(
                user.getId(),
                user.getEmail(),
                user.getEmailVerified() != null ? user.getEmailVerified() : false,
                verifiedAt
        );
    }

    // ------------------------------ HELPERS ------------------------------------------
    private Map<String, Object> buildUserClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        return claims;
    }

    private String createAccessToken(User user) {
        return jwtTokenProvider.createAccessToken(user.getEmail(), buildUserClaims(user));
    }
    private String createRefreshToken(User user) {
        return jwtTokenProvider.createRefreshToken(user.getEmail(), buildUserClaims(user));
    }

    private void applyAccessTokenHeader(HttpServletResponse response, String accessToken) {
        response.setHeader("Authorization", "Bearer " + accessToken);
    }

    private Cookie buildRefreshTokenCookie(String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(jwtProperties.getCookie().isSecure());
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtProperties.getTokenExpiration().getRefresh() * 24 * 60 * 60));
        return cookie;
    }

    private boolean isExpired(LocalDateTime expiredAt) {
        LocalDateTime now = LocalDateTime.now();
        Duration remaining = Duration.between(now, expiredAt);
        // 만료 시간이 지금보다 과거면 음수 or 0
        return !remaining.isPositive(); // 0 또는 음수면 만료로 봄
    }
}

