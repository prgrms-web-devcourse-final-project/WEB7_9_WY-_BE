package back.kalender.global.security.jwt;

import back.kalender.global.common.constant.HttpHeaders;
import back.kalender.global.common.constant.SecurityConstants;
import back.kalender.global.exception.ServiceException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// JWT 토큰 기반 인증 필터
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final Environment environment;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    // SecurityConfig의 publicPaths Bean 주입 (permitAll 경로는 토큰 없어도 정상이므로 WARN 로그 제외)
    private final List<String> publicPaths;
    
    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider, Environment environment, List<String> publicPaths) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.environment = environment;
        this.publicPaths = publicPaths;
    }

    private boolean isProdProfile() {
        return java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
    
    // 요청 경로가 permitAll 경로인지 확인
    private boolean isPublicPath(String path) {
        return publicPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    // 요청에서 디버깅용 메타데이터 추출
    private RequestMetadata extractRequestMetadata(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String xForwardedProto = request.getHeader("X-Forwarded-Proto");
        String host = request.getServerName();
        if (StringUtils.hasText(request.getHeader("Host"))) {
            host = request.getHeader("Host");
        }
        boolean hasCookie = StringUtils.hasText(request.getHeader("Cookie"));
        return new RequestMetadata(origin, xForwardedProto, host, hasCookie);
    }

    // 요청 메타데이터를 담는 내부 클래스
    private static class RequestMetadata {
        final String origin;
        final String xForwardedProto;
        final String host;
        final boolean hasCookie;

        RequestMetadata(String origin, String xForwardedProto, String host, boolean hasCookie) {
            this.origin = origin;
            this.xForwardedProto = xForwardedProto;
            this.host = host;
            this.hasCookie = hasCookie;
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String method = request.getMethod();
        
        // OPTIONS(preflight) 요청은 early return
        if ("OPTIONS".equalsIgnoreCase(method)) {
            if (!isProdProfile()) {
                log.debug("[JwtAuthFilter] OPTIONS preflight 요청 - path: {}", request.getRequestURI());
            }
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean hasAuthHeader = StringUtils.hasText(authHeader);
        boolean hasBearerPrefix = hasAuthHeader && authHeader.startsWith(HttpHeaders.BEARER_PREFIX);

        String token = resolveToken(request);

        // 토큰 없음/추출 실패 - permitAll 경로가 아닐 때만 WARN 로그
        if (!StringUtils.hasText(token)) {
            if (!isPublicPath(path)) {
                RequestMetadata metadata = extractRequestMetadata(request);
                log.warn("[JwtAuthFilter] 토큰 없음/추출 실패 - path: {}, method: {}, hasAuthHeader: {}, hasBearerPrefix: {}, origin: {}, xForwardedProto: {}, host: {}, hasCookie: {}",
                        path, method, hasAuthHeader, hasBearerPrefix, metadata.origin, metadata.xForwardedProto, metadata.host, metadata.hasCookie);
            }

            filterChain.doFilter(request, response);
            return;
        }

        // 토큰 검증 시도
        try {
            jwtTokenProvider.validateToken(token);

            try {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // 인증 성공은 dev에서만 DEBUG 로그
                if (!isProdProfile()) {
                    log.debug("[JwtAuthFilter] 인증 성공 - path: {}, userId: {}",
                            path, authentication.getName());
                }
            } catch (ServiceException e) {
                // 사용자 조회 실패 - request attribute에 저장하여 SecurityUtil에서 처리
                RequestMetadata metadata = extractRequestMetadata(request);
                log.warn("[JwtAuthFilter] 사용자 조회 실패 - path: {}, method: {}, errorCode: {}, origin: {}, xForwardedProto: {}, host: {}, hasCookie: {}",
                        path, method, e.getErrorCode().getCode(), metadata.origin, metadata.xForwardedProto, metadata.host, metadata.hasCookie);
                request.setAttribute(SecurityConstants.AUTH_FILTER_EXCEPTION_ATTR, e);
            }
        } catch (ServiceException e) {
            // 토큰 검증 실패 - 운영에서도 WARN 로그 (Authentication 미설정 → 401 발생)
            RequestMetadata metadata = extractRequestMetadata(request);
            log.warn("[JwtAuthFilter] JWT 토큰 검증 실패 - path: {}, method: {}, errorCode: {}, origin: {}, xForwardedProto: {}, host: {}, hasCookie: {}",
                    path, method, e.getErrorCode().getCode(), metadata.origin, metadata.xForwardedProto, metadata.host, metadata.hasCookie);
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
