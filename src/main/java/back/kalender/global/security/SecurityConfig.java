package back.kalender.global.security;

import back.kalender.global.common.constant.HttpHeaders;
import back.kalender.global.security.jwt.JwtAuthEntryPoint;
import back.kalender.global.security.jwt.JwtAuthFilter;
import back.kalender.global.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Spring Security 설정
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${custom.site.frontUrl}")
    private String frontUrl;
    
    private final Environment environment;
    
    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }
    
    private boolean isProdProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 공개 경로 목록 Bean - SecurityConfig와 JwtAuthFilter에서 공통으로 사용
     */
    @Bean
    public List<String> publicPaths() {
        List<String> paths = new ArrayList<>(Arrays.asList(
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
                "/ws-chat/**",                     // WebSocket 연결 허용
                "/payment-test.html",               // 결제 테스트 페이지
                "/payment/**",                      // 결제 관련 정적 파일
                "/api/v1/payments/client-key",     // 결제 클라이언트 키 조회 (인증 불필요)
                "/actuator/prometheus"
        ));
        
        // 개발 환경에서만 H2 콘솔 허용
        if (!isProdProfile()) {
            paths.add("/h2-console/**");
        }
        
        return Collections.unmodifiableList(paths);
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtTokenProvider jwtTokenProvider, Environment environment, List<String> publicPaths) {
        return new JwtAuthFilter(jwtTokenProvider, environment, publicPaths);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        if (isProdProfile()) {
            configuration.setAllowedOriginPatterns(Arrays.asList(
                frontUrl,
                "https://web-7-9-wy-fe*.vercel.app",  // preview1
                "https://web-7-9-wy-fe-*.vercel.app", // preview2
                "https://web-7-9-wy-fe.vercel.app"   // production
            ));
        } else {
            // 개발: 로컬 및 개발 도메인 허용
            List<String> allowedOrigins = new ArrayList<>();
            allowedOrigins.add("http://localhost:3000");
            allowedOrigins.add(frontUrl);
            configuration.setAllowedOrigins(allowedOrigins);
        }

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 인증 정보 포함 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);
        
        // preflight 요청 결과를 캐시할 시간 (초)
        configuration.setMaxAge(3600L);
        
        // 노출할 응답 헤더
        configuration.setExposedHeaders(Arrays.asList(HttpHeaders.AUTHORIZATION));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter,
            JwtAuthEntryPoint jwtAuthEntryPoint,
            List<String> publicPaths
    ) throws Exception {
        
        http
                // CSRF 비활성화 (JWT 기반 stateless 인증에서는 CSRF 보호 불필요)
                .csrf(csrf -> csrf.disable())
                
                // 기본 인증 방식 비활성화 (JWT만 사용)
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                
                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // 세션 관리 정책: STATELESS (JWT 사용 시 세션을 사용하지 않음)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // JWT 인증 필터 추가
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                
                // 예외 처리 (인증 실패 시)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthEntryPoint))
                
                // X-Frame-Options 설정
                .headers(headers -> {
                    if (isProdProfile()) {
                        // 프로덕션: DENY (클릭재킹 방지)
                        headers.frameOptions(frame -> frame.deny());
                    } else {
                        // 개발: sameOrigin (H2 콘솔 사용을 위해)
                        headers.frameOptions(frame -> frame.sameOrigin());
                    }
                })
                
                // 요청 인가 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicPaths.toArray(new String[0]))
                        .permitAll()
                        // CORS preflight 요청(OPTIONS)은 인증 없이 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}