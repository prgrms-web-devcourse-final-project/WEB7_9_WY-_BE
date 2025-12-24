package back.kalender.global.security.jwt;

import back.kalender.global.common.constant.HttpHeaders;
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
 * 
 * <p>요청의 Authorization 헤더에서 JWT 토큰을 추출하고 검증하여
 * SecurityContext에 Authentication을 설정합니다.
 * 
 * <p>처리 흐름:
 * <ol>
 *   <li>JWT 토큰 검증: 토큰의 유효성(만료, 서명, 형식) 확인</li>
 *   <li>Authentication 생성: 토큰에서 사용자 정보 조회 및 Authentication 객체 생성</li>
 *   <li>SecurityContext 설정: 인증 성공 시 SecurityContext에 Authentication 저장</li>
 * </ol>
 * 
 * <p>예외 처리:
 * <ul>
 *   <li>토큰 검증 실패: 인증 실패로 간주, AuthenticationEntryPoint가 401 반환</li>
 *   <li>사용자 조회 실패: 비즈니스 예외로 간주, request attribute에 저장 후 SecurityUtil에서 처리</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * Request attribute 키: 인증 필터에서 발생한 비즈니스 예외를 저장
     * SecurityUtil에서 이 예외를 확인하여 GlobalExceptionHandler가 적절한 상태 코드로 처리하도록 함
     */
    private static final String AUTH_FILTER_EXCEPTION_ATTR = "AUTH_FILTER_EXCEPTION";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                // 1단계: JWT 토큰 검증 (만료, 서명, 형식 등)
                jwtTokenProvider.validateToken(token);
                
                // 2단계: Authentication 객체 생성 (사용자 조회 포함)
                try {
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (ServiceException e) {
                    // 토큰은 유효하지만 사용자 조회 실패 (예: USER_NOT_FOUND)
                    // 인증 실패가 아닌 비즈니스 예외이므로 request attribute에 저장
                    // SecurityUtil.getCurrentUserIdOrThrow()에서 이 예외를 재발생시켜
                    // GlobalExceptionHandler가 적절한 상태 코드(예: 404)로 처리
                    log.warn("[JwtAuthFilter] 사용자 조회 실패 - errorCode: {}, message: {}", 
                            e.getErrorCode().getCode(), e.getErrorCode().getMessage());
                    request.setAttribute(AUTH_FILTER_EXCEPTION_ATTR, e);
                    // SecurityContext에는 Authentication을 설정하지 않음
                }
            } catch (ServiceException e) {
                // JWT 토큰 검증 실패 (만료, 서명 오류, 형식 오류 등)
                // 인증 실패로 간주하여 필터에서 예외를 무시하고 계속 진행
                // Spring Security가 SecurityContext에 Authentication이 없음을 확인하고
                // AuthenticationEntryPoint를 호출하여 401 반환
                log.debug("[JwtAuthFilter] JWT 토큰 검증 실패 - errorCode: {}", e.getErrorCode().getCode());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰을 추출합니다.
     * 
     * @param request HTTP 요청
     * @return JWT 토큰 문자열, 없으면 null
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(HttpHeaders.BEARER_PREFIX)) {
            return header.substring(HttpHeaders.BEARER_PREFIX.length());
        }
        return null;
    }
}
