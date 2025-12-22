package back.kalender.global.security.webSocket;

import back.kalender.global.common.constant.HttpHeaders;
import back.kalender.global.exception.ServiceException;
import back.kalender.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * WebSocket 연결 시 JWT 인증을 처리하는 인터셉터
 * STOMP CONNECT 명령 시 Authorization 헤더에서 JWT 토큰을 추출하여 검증
 */
@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final Environment environment;
    
    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }
    
    /**
     * JWT 토큰을 마스킹하여 반환 (프로덕션 환경에서 사용)
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class);

        // CONNECT 명령일 때만 인증 처리
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor);

            if (!StringUtils.hasText(token)) {
                log.warn("WebSocket 인증 실패 - JWT 토큰이 없음");
                throw new IllegalArgumentException("JWT 토큰이 필요합니다");
            }

            try {
                // JWT 토큰 검증 (구체적인 예외 발생)
                jwtTokenProvider.validateToken(token);
                // JWT에서 Authentication 객체 생성
                Authentication authentication = jwtTokenProvider.getAuthentication(token);

                // WebSocket 세션에 인증 정보 설정
                accessor.setUser(authentication);

                log.info("WebSocket 인증 성공 - user: {}", authentication.getName());
            } catch (ServiceException e) {
                if (isDevProfile()) {
                    log.error("WebSocket 인증 실패 - token: {}, error: {}",
                            token, e.getErrorCode().getMessage());
                } else {
                    log.error("WebSocket 인증 실패 - token: {}, error: {}",
                            maskToken(token), e.getErrorCode().getMessage());
                }
                throw new IllegalArgumentException("유효하지 않은 JWT 토큰입니다: " + e.getErrorCode().getMessage());
            } catch (Exception e) {
                if (isDevProfile()) {
                    log.error("WebSocket 인증 실패 - token: {}, error: {}",
                            token, e.getMessage());
                } else {
                    log.error("WebSocket 인증 실패 - token: {}, error: {}",
                            maskToken(token), e.getMessage());
                }
                throw new IllegalArgumentException("JWT 토큰 처리 중 오류가 발생했습니다");
            }
        }

        return message;
    }

    /**
     * STOMP 헤더에서 JWT 토큰 추출
     */
    private String resolveToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(HttpHeaders.BEARER_PREFIX)) {
            return authHeader.substring(HttpHeaders.BEARER_PREFIX.length());
        }

        return null;
    }
}