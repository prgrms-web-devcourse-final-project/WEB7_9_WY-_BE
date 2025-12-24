package back.kalender.global.security.jwt;

import back.kalender.global.common.constant.HttpHeaders;
import back.kalender.global.common.constant.SecurityConstants;
import back.kalender.global.exception.ServiceException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 토큰 기반 인증 필터
 * Authorization 헤더에서 JWT 토큰을 추출하고 검증하여 SecurityContext에 Authentication을 설정합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                jwtTokenProvider.validateToken(token);
                
                try {
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (ServiceException e) {
                    // 사용자 조회 실패는 비즈니스 예외이므로 request attribute에 저장하여 SecurityUtil에서 처리
                    log.warn("[JwtAuthFilter] 사용자 조회 실패 - errorCode: {}, message: {}", 
                            e.getErrorCode().getCode(), e.getErrorCode().getMessage());
                    request.setAttribute(SecurityConstants.AUTH_FILTER_EXCEPTION_ATTR, e);
                }
            } catch (ServiceException e) {
                // 토큰 검증 실패는 인증 실패로 간주하여 AuthenticationEntryPoint가 401 반환
                log.debug("[JwtAuthFilter] JWT 토큰 검증 실패 - errorCode: {}", e.getErrorCode().getCode());
            }
        }

        filterChain.doFilter(request, response);
    }

    // Authorization 헤더에서 Bearer 토큰 추출
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(HttpHeaders.BEARER_PREFIX)) {
            return header.substring(HttpHeaders.BEARER_PREFIX.length());
        }
        return null;
    }
}
