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
import java.util.Arrays;
import java.util.List;

/**
 * JWT 토큰 기반 인증 필터
 * Authorization 헤더에서 JWT 토큰을 추출하고 검증하여 SecurityContext에 Authentication을 설정합니다.
 */
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final Environment environment;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // SecurityConfig의 publicPaths와 동일한 목록 (필드로 캐싱 - 생성자에서 1회만 초기화)
    // permitAll 경로는 토큰이 없어도 정상이므로 WARN 로그를 남기지 않음
    private final List<String> publicPaths;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider, Environment environment) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.environment = environment;
        this.publicPaths = initializePublicPaths();
    }

    private boolean isProdProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    /**
     * SecurityConfig의 publicPaths와 동일한 목록 초기화 (생성자에서 1회만 실행)
     * 주의: 프로파일은 애플리케이션 시작 시 결정되므로 런타임 변경은 고려하지 않음
     */
    private List<String> initializePublicPaths() {
        List<String> paths = new java.util.ArrayList<>(Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/password/send",
            "/api/v1/auth/password/reset",
            "/api/v1/auth/email/send",
            "/api/v1/auth/email/verify",
            "/api/v1/user",                    // 회원가입
            "/api/v1/schedule/by-artists",      //비회원 공개 일정 조회
            "/api/v1/artist",               //비회원 아티스트 정보 조회
            "/favicon.ico",
            "/swagger-ui/**",                  // Swagger UI
            "/v3/api-docs/**",                 // OpenAPI 문서
            "/swagger-resources/**",            // Swagger 리소스
            "/api/v1/notifications/**",          // 알림
            "/ws-chat/**",                     // WebSocket 연결 허용
            "/payment-test.html",               // 결제 테스트 페이지
            "/payment/**",                      // 결제 관련 정적 파일
            "/api/v1/payments/client-key"      // 결제 클라이언트 키 조회 (인증 불필요)
        ));

        // 개발 환경에서만 H2 콘솔 허용
        if (!isProdProfile()) {
            paths.add("/h2-console/**");
        }

        return java.util.Collections.unmodifiableList(paths);
    }

    /**
     * 요청 경로가 permitAll 경로인지 확인
     */
    private boolean isPublicPath(String path) {
        return publicPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String method = request.getMethod();

        // 1) OPTIONS(preflight) 요청은 early return (로그 노이즈 방지)
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

        // 2) 토큰 없음/추출 실패 케이스 - permitAll 경로가 아닐 때만 WARN 로그
        if (!StringUtils.hasText(token)) {
            // permitAll 경로는 토큰이 없어도 정상이므로 로그 노이즈 방지
            if (!isPublicPath(path)) {
                String origin = request.getHeader("Origin");
                String xForwardedProto = request.getHeader("X-Forwarded-Proto");
                String host = request.getServerName();
                if (StringUtils.hasText(request.getHeader("Host"))) {
                    host = request.getHeader("Host");
                }
                boolean hasCookie = StringUtils.hasText(request.getHeader("Cookie"));

                log.warn("[JwtAuthFilter] 토큰 없음/추출 실패 - path: {}, method: {}, hasAuthHeader: {}, hasBearerPrefix: {}, origin: {}, xForwardedProto: {}, host: {}, hasCookie: {}",
                        path, method, hasAuthHeader, hasBearerPrefix, origin, xForwardedProto, host, hasCookie);
            }

            filterChain.doFilter(request, response);
            return;
        }

        // 3) 토큰 검증 시도
        try {
            jwtTokenProvider.validateToken(token);

            try {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 4) 인증 성공은 dev에서만 DEBUG
                if (!isProdProfile()) {
                    log.debug("[JwtAuthFilter] 인증 성공 - path: {}, userId: {}",
                            path, authentication.getName());
                }
            } catch (ServiceException e) {
                // 사용자 조회 실패는 비즈니스 예외이므로 request attribute에 저장하여 SecurityUtil에서 처리
                String origin = request.getHeader("Origin");
                String xForwardedProto = request.getHeader("X-Forwarded-Proto");
                String host = request.getServerName();
                if (StringUtils.hasText(request.getHeader("Host"))) {
                    host = request.getHeader("Host");
                }
                boolean hasCookie = StringUtils.hasText(request.getHeader("Cookie"));

                log.warn("[JwtAuthFilter] 사용자 조회 실패 - path: {}, method: {}, errorCode: {}, origin: {}, xForwardedProto: {}, host: {}, hasCookie: {}",
                        path, method, e.getErrorCode().getCode(), origin, xForwardedProto, host, hasCookie);
                request.setAttribute(SecurityConstants.AUTH_FILTER_EXCEPTION_ATTR, e);
            }
        } catch (ServiceException e) {
            // 3) 토큰 검증 실패 - 운영에서도 WARN 로그 (메타 정보 포함)
            String origin = request.getHeader("Origin");
            String xForwardedProto = request.getHeader("X-Forwarded-Proto");
            String host = request.getServerName();
            if (StringUtils.hasText(request.getHeader("Host"))) {
                host = request.getHeader("Host");
            }
            boolean hasCookie = StringUtils.hasText(request.getHeader("Cookie"));

            log.warn("[JwtAuthFilter] JWT 토큰 검증 실패 - path: {}, method: {}, errorCode: {}, origin: {}, xForwardedProto: {}, host: {}, hasCookie: {}",
                    path, method, e.getErrorCode().getCode(), origin, xForwardedProto, host, hasCookie);
            // Authentication 설정하지 않음 → SecurityContext에 없음 → 401 발생
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
