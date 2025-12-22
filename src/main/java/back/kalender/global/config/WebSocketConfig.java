package back.kalender.global.config;

import back.kalender.global.security.jwt.JwtTokenProvider;
import back.kalender.global.security.webSocket.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

/**
 * WebSocket 설정
 * STOMP 프로토콜을 사용한 실시간 채팅 기능
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final Environment environment;

    @Value("${custom.site.frontUrl}")
    private String frontUrl;

    private boolean isProdProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    /**
     * 메시지 브로커 설정
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트로 메시지를 보낼 때 사용할 prefix
        // /topic: 1:N 브로드캐스트 (채팅방 전체)
        config.enableSimpleBroker("/topic");

        // 클라이언트가 서버로 메시지를 보낼 때 사용할 prefix
        // 예: /app/chat.send/{partyId}
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * STOMP 엔드포인트 등록
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        var endpoint = registry.addEndpoint("/ws-chat");
        
        if (isProdProfile()) {
            // 프로덕션: 운영 도메인만 허용
            endpoint.setAllowedOrigins(frontUrl);
        } else {
            // 개발: 로컬 및 개발 도메인 허용
            endpoint.setAllowedOriginPatterns("*");
        }
        
        endpoint.withSockJS(); // SockJS fallback 옵션 활성화
    }

    /**
     * 클라이언트로부터 들어오는 메시지에 대한 채널 설정
     * JWT 인증 인터셉터 추가
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new WebSocketAuthInterceptor(jwtTokenProvider, environment));
    }
}