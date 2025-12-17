package back.kalender.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationControllerSpec {

    @Operation(
            summary = "알림 구독 (SSE 연결)",
            description = """
                    서버와 SSE(Server-Sent Events) 연결을 맺습니다.
                    
                    **[연결 방식]**
                    1. 헤더에 `Authorization: Bearer {Token}`을 포함하여 요청합니다.
                    2. 성공 시 `text/event-stream` 형식으로 연결이 유지됩니다.
                    3. 최초 연결 시 503 에러 방지를 위한 더미 데이터(`connect` 이벤트)가 발송됩니다.
                    
                    **[주의사항]**
                    * Nginx 등 프록시 사용 시 `proxy_buffering off` 설정이 필요합니다.
                    * 클라이언트는 `EventSource` 또는 `event-source-polyfill`을 사용하여 연결해야 합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "SSE 연결 성공 (스트림 시작)",
                    content = @Content(
                            mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                            schema = @Schema(implementation = SseEmitter.class),
                            examples = @ExampleObject(
                                    name = "SSE 이벤트 스트림 예시",
                                    summary = "연결 성공 및 알림 수신 예시",
                                    description = "실제 데이터는 `data` 필드에 JSON 문자열로 담겨옵니다.",
                                    value = """
                                            id: 1
                                            event: connect
                                            data: "EventStream Created. [userId=1]"
                                            
                                            id: 1
                                            event: notification
                                            data: {
                                              "id": 10,
                                              "type": "APPLY",
                                              "title": "파티 신청 알림",
                                              "content": "홍길동(24/남)님이 파티를 신청했습니다.",
                                              "targetUrl": "/parties/100",
                                              "isRead": false,
                                              "createdAt": "2024-12-17T10:00:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (토큰 없음 또는 만료)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "AUTH_001",
                                      "message": "인증 정보가 유효하지 않습니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "서버 연결 초과",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "SSE_001",
                                      "message": "서버의 최대 연결 수를 초과하여 더 이상 연결할 수 없습니다."
                                    }
                                    """)
                    )
            )
    })
    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    public SseEmitter subscribeToNotifications(
            @AuthenticationPrincipal(expression = "userId") Long userId
    );
}
