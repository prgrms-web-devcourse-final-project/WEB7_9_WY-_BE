package back.kalender.global.security;

import back.kalender.global.security.jwt.JwtAuthEntryPoint;
import back.kalender.global.security.jwt.JwtAuthFilter;
import back.kalender.global.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 설정
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${custom.site.frontUrl}")
    private String frontUrl;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtTokenProvider jwtTokenProvider) {
        return new JwtAuthFilter(jwtTokenProvider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 프론트엔드 URL 허용
        configuration.setAllowedOrigins(List.of(frontUrl));
        
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 인증 정보 포함 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);
        
        // preflight 요청 결과를 캐시할 시간 (초)
        configuration.setMaxAge(3600L);
        
        // 노출할 응답 헤더
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter,
            JwtAuthEntryPoint jwtAuthEntryPoint
    ) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용 시 세션을 사용하지 않으므로)
                .csrf(csrf -> csrf.disable())
                
                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // 세션 관리 정책: STATELESS (JWT 사용 시 세션을 사용하지 않음)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // JWT 인증 필터 추가
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                
                // 예외 처리 (인증 실패 시)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthEntryPoint))
                
                // X-Frame-Options 설정 (H2 콘솔 사용을 위해 sameOrigin)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                )
                
                // 요청 인가 설정
                .authorizeHttpRequests(auth -> auth
                        // 공개 엔드포인트 (인증 불필요)
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/password/send",
                                "/api/v1/auth/password/reset",
                                "/api/v1/auth/email/send",
                                "/api/v1/auth/email/verify",
                                "/api/v1/user",                    // 회원가입
                                "/api/v1/schedule/public/**",      // 공개 일정 조회
                                "/api/v1/artist/**",               // 아티스트 정보 조회
                                "/h2-console/**",                  // H2 콘솔
                                "/favicon.ico",
                                "/swagger-ui/**",                  // Swagger UI
                                "/v3/api-docs/**",                 // OpenAPI 문서
                                "/swagger-resources/**",            // Swagger 리소스
                                "/api/v1/performance-seats/**",
                                "/api/v1/queue/**",
                                "/ws-chat/**"                      // WebSocket 연결 허용
                        ).permitAll()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}