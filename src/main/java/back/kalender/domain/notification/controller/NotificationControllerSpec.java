package back.kalender.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationControllerSpec {

    @Operation(
            summary = "알림 구독",
            description = "서버-센트 이벤트(SSE)를 통해 실시간 알림을 구독합니다. " +
                    "클라이언트는 이 엔드포인트에 연결하여 새로운 알림을 실시간으로 수신할 수 있습니다."
    )
    @ApiResponse(responseCode = "200", description = "SSE 연결 성공")
    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    public SseEmitter subscribeToNotifications(
            @AuthenticationPrincipal(expression = "userId") Long userId
    );
}
