package back.kalender.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final UserDetailsService userDetailsService;

    private SecretKey signingKey;
    private long accessTokenValidityInMillis;
    private long refreshTokenValidityInMillis;

    @PostConstruct
    public void init() {
        // secret 문자열로 HMAC 키 생성
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityInMillis = jwtProperties.getTokenExpiration().getAccess() * 1000;
        this.refreshTokenValidityInMillis = jwtProperties.getTokenExpiration().getRefresh() * 24 * 60 * 60 * 1000;
    }

    public String createAccessToken(String subject, Map<String, Object> additionalClaims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValidityInMillis);

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

    public String createRefreshToken(String subject, Map<String, Object> additionalClaims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenValidityInMillis);

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

    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            return false;
        } catch (ExpiredJwtException e) {
            return false;
        } catch (UnsupportedJwtException e) {
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String getSubject(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public Authentication getAuthentication(String token) {
        String username = getSubject(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    public Long getUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        } else if (userIdObj instanceof Number) {
            return ((Number) userIdObj).longValue();
        }
        return null;
    }
}
