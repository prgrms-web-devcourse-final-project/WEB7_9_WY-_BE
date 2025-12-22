package back.kalender.global.security.jwt;

import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.repository.UserRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import back.kalender.global.security.user.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        // secret 문자열로 HMAC 키 생성
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(String subject, long validityInMillis) {
        return createToken(subject, null, validityInMillis);
    }

    public String createToken(String subject, Map<String, Object> additionalClaims, long validityInMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMillis);

        JwtBuilder builder = Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey);

        if (additionalClaims != null && !additionalClaims.isEmpty()) {
            builder.addClaims(additionalClaims);
        }

        return builder.compact();
    }

    // JWT 토큰 검증 (실패 시 구체적인 예외 발생)
    public void validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("[JWT] [ValidateToken] 토큰이 null이거나 비어있음");
            throw new ServiceException(ErrorCode.JWT_TOKEN_NULL_OR_EMPTY);
        }

        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseClaimsJws(token);
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] [ValidateToken] 만료된 토큰 - message={}", e.getMessage());
            throw new ServiceException(ErrorCode.JWT_TOKEN_EXPIRED);
        } catch (MalformedJwtException e) {
            log.warn("[JWT] [ValidateToken] 형식 오류 - message={}", e.getMessage());
            throw new ServiceException(ErrorCode.JWT_TOKEN_MALFORMED);
        } catch (SecurityException | io.jsonwebtoken.security.SignatureException e) {
            log.warn("[JWT] [ValidateToken] 서명 오류 - message={}", e.getMessage());
            throw new ServiceException(ErrorCode.JWT_TOKEN_SIGNATURE_INVALID);
        } catch (UnsupportedJwtException e) {
            log.warn("[JWT] [ValidateToken] 지원하지 않는 토큰 - message={}", e.getMessage());
            throw new ServiceException(ErrorCode.JWT_TOKEN_UNSUPPORTED);
        } catch (IllegalArgumentException e) {
            log.warn("[JWT] [ValidateToken] 토큰 파싱 실패 - message={}", e.getMessage());
            throw new ServiceException(ErrorCode.JWT_TOKEN_ILLEGAL_ARGUMENT);
        } catch (JwtException e) {
            log.warn("[JWT] [ValidateToken] JWT 예외 - message={}", e.getMessage());
            throw new ServiceException(ErrorCode.JWT_TOKEN_MALFORMED);
        }
    }

    // JWT 토큰에서 subject 추출 (검증 후)
    public String getSubject(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] [GetSubject] 만료된 토큰 - message={}", e.getMessage());
            throw new ServiceException(ErrorCode.JWT_TOKEN_EXPIRED);
        } catch (MalformedJwtException e) {
            log.warn("[JWT] [GetSubject] 형식 오류 - message={}", e.getMessage());
            throw new ServiceException(ErrorCode.JWT_TOKEN_MALFORMED);
        } catch (SecurityException | io.jsonwebtoken.security.SignatureException e) {
            log.warn("[JWT] [GetSubject] 서명 오류 - message={}", e.getMessage());
            throw new ServiceException(ErrorCode.JWT_TOKEN_SIGNATURE_INVALID);
        } catch (UnsupportedJwtException e) {
            log.warn("[JWT] [GetSubject] 지원하지 않는 토큰 - message={}", e.getMessage());
            throw new ServiceException(ErrorCode.JWT_TOKEN_UNSUPPORTED);
        } catch (IllegalArgumentException e) {
            log.warn("[JWT] [GetSubject] 토큰 파싱 실패 - message={}", e.getMessage());
            throw new ServiceException(ErrorCode.JWT_TOKEN_ILLEGAL_ARGUMENT);
        } catch (JwtException e) {
            log.warn("[JWT] [GetSubject] JWT 예외 - message={}", e.getMessage());
            throw new ServiceException(ErrorCode.JWT_TOKEN_MALFORMED);
        }
    }

    public Authentication getAuthentication(String token) {
        String userIdStr = getSubject(token);
        Long userId = Long.parseLong(userIdStr);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
        
        List<GrantedAuthority> authorities = createAuthorities("ROLE_USER");
        
        CustomUserDetails userDetails = new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                authorities
        );

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    private List<GrantedAuthority> createAuthorities(String roleName) {
        return Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }

    public Long getUserId(String token) {
        String userIdStr = getSubject(token);
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
