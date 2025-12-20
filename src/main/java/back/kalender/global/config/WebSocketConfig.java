package back.kalender.global.config;

import back.kalender.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 설정
 * STOMP 프로토콜을 사용한 실시간 채팅 기능
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;

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
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*") // CORS 설정
                .withSockJS(); // SockJS fallback 옵션 활성화
    }

    /**
     * 클라이언트로부터 들어오는 메시지에 대한 채널 설정
     * JWT 인증 인터셉터 추가
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new back.kalender.domain.chat.config.WebSocketAuthInterceptor(jwtTokenProvider));
    }
}