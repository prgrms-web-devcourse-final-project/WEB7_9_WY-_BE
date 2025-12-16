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

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final int EMAIL_VERIFICATION_RESEND_LIMIT_MINUTES = 5;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

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

        // 토큰 생성, 저장 및 응답 설정
        // 주의: 기존 refresh token을 삭제하지 않으므로 여러 기기에서 동시 로그인 가능
        createAndSaveTokens(user, response);

        return new UserLoginResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                user.getProfileImage(),
                user.isEmailVerified()
        );
    }

    @Override
    @Transactional
    public void logout(String refreshToken, HttpServletResponse response) {
        // 해당 기기의 refresh token만 삭제 (다른 기기는 영향 없음)
        if (refreshToken != null) {
            refreshTokenRepository.findByToken(refreshToken)
                    .ifPresent(refreshTokenRepository::delete);
        }
        
        // Refresh Token 쿠키 삭제
        clearRefreshTokenCookie(response);
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
        if (refreshTokenEntity.isExpired()) {
            refreshTokenRepository.delete(refreshTokenEntity);
            throw new ServiceException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        // 유저 조회
        User user = userRepository.findById(refreshTokenEntity.getUserId())
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        // 기존 Refresh Token 삭제 (토큰 로테이션: 보안을 위해 사용된 토큰은 즉시 무효화)
        refreshTokenRepository.delete(refreshTokenEntity);

        // 새 토큰 생성, 저장 및 응답 설정
        createAndSaveTokens(user, response);
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

        // 이메일 발송
        emailService.sendPasswordResetEmail(user.getEmail(), token);
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
        if (resetToken.isExpired()) {
            throw new ServiceException(ErrorCode.EXPIRED_PASSWORD_RESET_TOKEN);
        }

        // 유저 조회 및 비밀번호 업데이트
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        user.updatePassword(passwordEncoder.encode(request.newPassword()));

        // 토큰 사용 처리 (updatedAt 자동 업데이트를 위해 명시적 저장)
        resetToken.markUsed();
        passwordResetTokenRepository.save(resetToken);
    }

    @Override
    @Transactional
    public void sendVerifyEmail(VerifyEmailSendRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        // 이미 인증된 경우
        if (user.isEmailVerified()) {
            throw new ServiceException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        // 최근 N분 이내 발송된 인증 코드 확인 (재발송 제한)
        emailVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .ifPresent(verification -> {
                    if (verification.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(EMAIL_VERIFICATION_RESEND_LIMIT_MINUTES))) {
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

        // 이메일 발송
        emailService.sendVerificationEmail(user.getEmail(), code);
    }

    @Override
    @Transactional
    public VerifyEmailResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        // 인증 코드 조회 
        EmailVerification verification = emailVerificationRepository
                .findByUserIdAndCode(user.getId(), request.code())
                .orElseThrow(() -> new ServiceException(ErrorCode.EMAIL_VERIFICATION_CODE_NOT_FOUND));

        // 사용 여부 확인
        if (verification.isUsed()) {
            throw new ServiceException(ErrorCode.INVALID_EMAIL_VERIFICATION_CODE);
        }

        // 만료 확인
        if (verification.isExpired()) {
            throw new ServiceException(ErrorCode.EXPIRED_EMAIL_VERIFICATION_CODE);
        }

        // 인증 처리 (updatedAt 자동 업데이트를 위해 명시적 저장)
        verification.markUsed();
        emailVerificationRepository.save(verification);
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
                user.isEmailVerified(),
                verifiedAt
        );
    }

    // ------------------------------ HELPERS ------------------------------------------
    /**
     * Access Token과 Refresh Token을 생성하고 저장합니다.
     * 여러 기기 로그인을 허용하므로 기존 refresh token을 삭제하지 않습니다.
     */
    private void createAndSaveTokens(User user, HttpServletResponse response) {
        // 토큰 생성
        String accessToken = createAccessToken(user);
        String refreshToken = createRefreshToken(user);

        // Refresh Token DB 저장 (기존 토큰은 삭제하지 않음 - 여러 기기 로그인 허용)
        RefreshToken refreshTokenEntity = RefreshToken.create(
                user.getId(),
                refreshToken,
                jwtProperties.getTokenExpiration().getRefresh()
        );
        refreshTokenRepository.save(refreshTokenEntity);

        // 토큰을 Response에 설정
        setTokensToResponse(response, accessToken, refreshToken);
    }

    private String createAccessToken(User user) {
        long validityInMillis = jwtProperties.getTokenExpiration().getAccessInMillis();
        return jwtTokenProvider.createToken(String.valueOf(user.getId()), validityInMillis);
    }
    
    private String createRefreshToken(User user) {
        long validityInMillis = jwtProperties.getTokenExpiration().getRefreshInMillis();
        return jwtTokenProvider.createToken(String.valueOf(user.getId()), validityInMillis);
    }

    private void setTokensToResponse(HttpServletResponse response, String accessToken, String refreshToken) {
        response.setHeader("Authorization", "Bearer " + accessToken);
        response.addCookie(buildRefreshTokenCookie(refreshToken));
    }

    private Cookie buildRefreshTokenCookie(String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(jwtProperties.getCookie().isSecure());
        cookie.setPath("/");
        cookie.setMaxAge((int) jwtProperties.getTokenExpiration().getRefreshInSeconds());
        return cookie;
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(jwtProperties.getCookie().isSecure());
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }


}

