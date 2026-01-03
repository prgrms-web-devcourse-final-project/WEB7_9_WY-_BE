package back.kalender.global.config;

import back.kalender.global.security.jwt.JwtTokenProvider;
import back.kalender.global.security.webSocket.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;  // 추가!
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

@Slf4j
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

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
        log.info("WebSocket 메시지 브로커 설정 완료");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        if (isProdProfile()) {
            registry.addEndpoint("/ws-chat")
                    .setAllowedOrigins(frontUrl)
                    .withSockJS();
            log.info("WebSocket 엔드포인트 등록 완료 (Production) - origin: {}", frontUrl);
        } else {
            registry.addEndpoint("/ws-chat")
                    .setAllowedOriginPatterns("*")
                    .withSockJS();
            log.info("WebSocket 엔드포인트 등록 완료 (Development) - allowedOriginPatterns: *");
        }
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new WebSocketAuthInterceptor(jwtTokenProvider, environment));
        log.info("WebSocket 인증 인터셉터 등록 완료");
    }
}