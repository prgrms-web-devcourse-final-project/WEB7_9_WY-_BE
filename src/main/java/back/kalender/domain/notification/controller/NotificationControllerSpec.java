package back.kalender.domain.notification.controller;

import back.kalender.domain.notification.response.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationControllerSpec {

    @Operation(
            summary = "알림 구독 (SSE 연결)",
            description = """
                    서버와 SSE(Server-Sent Events) 연결을 맺습니다.
                    
                    브라우저의 `EventSource` API는 HTTP 헤더 설정을 지원하지 않으므로,
                    **반드시 액세스 토큰을 쿼리 파라미터로 전달해야 합니다.**
                    
                    **[연결 방식]**
                    * **URL:** `/api/v1/notifications/subscribe?token={Access_Token}`
                    * **주의:** 토큰 앞의 `Bearer ` 접두사는 제거하고 순수 토큰 값만 넣어주세요.
                    
                    **[참고]**
                    * 성공 시 `text/event-stream` 형식으로 연결이 유지됩니다.
                    * 최초 연결 시 503 에러 방지를 위한 더미 데이터(`connect` 이벤트)가 발송됩니다.
                    * Nginx 버퍼링 방지 처리는 백엔드에서 자동으로 적용됩니다 (`X-Accel-Buffering: no`).
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
                                              "isRead": false,
                                              "createdAt": "2024-12-17T10:00:00",
                                              "partyId": 5,
                                              "applicationId": 3
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
                                      "code": "7004",
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
                                      "code": "503",
                                      "message": "서버의 최대 연결 수를 초과하여 더 이상 연결할 수 없습니다."
                                    }
                                    """)
                    )
            )
    })
    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    public SseEmitter subscribeToNotifications(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId,
            HttpServletResponse response
    );

    @Operation(
            summary = "알림 목록 조회",
            description = """
                    로그인한 사용자의 알림 목록을 최신순으로 조회합니다.
                    
                    * **페이징:** `page`(0부터 시작), `size` 파라미터를 지원합니다.
                    * **정렬:** 기본적으로 생성일자 기준 내림차순입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Page.class),
                            examples = @ExampleObject(
                                    name = "알림 목록 응답 예시",
                                    value = """
                                            {
                                              "content": [
                                                {
                                                  "notificationId": 12,
                                                  "notificationType": "EVENT_REMINDER",
                                                  "title": "오늘의 일정 알림",
                                                  "content": "오늘 18시에 BTS 콘서트 일정이 있습니다!",
                                                  "isRead": false,
                                                  "createdAt": "2025-12-25T09:00:00"
                                                },
                                                {
                                                  "notificationId": 11,
                                                  "notificationType": "ACCEPT",
                                                  "title": "파티 수락 알림",
                                                  "content": "'잠실 택시팟' 파티 신청이 수락되었습니다.",
                                                  "isRead": true,
                                                  "createdAt": "2025-12-24T14:30:00"
                                                },
                                                {
                                                  "notificationId": 12,
                                                  "notificationType": "APPLY",
                                                  "title": "새로운 파티 신청",
                                                  "content": "홍길동님이 신청했습니다.",
                                                  "isRead": false,
                                                  "createdAt": "2025-12-25T09:00:00",
                                                  "partyId": 100,
                                                  "applicationId": 50
                                                }
                                              ],
                                              "pageable": {
                                                "pageNumber": 0,
                                                "pageSize": 20,
                                                "sort": {
                                                  "empty": false,
                                                  "sorted": true,
                                                  "unsorted": false
                                                },
                                                "offset": 0,
                                                "paged": true,
                                                "unpaged": false
                                              },
                                              "last": true,
                                              "totalPages": 1,
                                              "totalElements": 2,
                                              "size": 20,
                                              "number": 0,
                                              "sort": {
                                                "empty": false,
                                                "sorted": true,
                                                "unsorted": false
                                              },
                                              "first": true,
                                              "numberOfElements": 2,
                                              "empty": false
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
                                      "code": "7004",
                                      "message": "인증 정보가 유효하지 않습니다."
                                    }
                                    """)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    );

    @Operation(
            summary = "알림 전체 읽음 처리",
            description = """
                    사용자의 **안 읽은 알림(`isRead: false`)**을 모두 **읽음(`isRead: true`)** 상태로 변경합니다.
                    
                    * 프론트엔드에서 '알림 버튼'을 클릭하여 목록을 열 때 이 API를 호출하면 배지 카운트를 초기화할 수 있습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "읽음 처리 성공 (Body 없음)",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "7004",
                                      "message": "인증 정보가 유효하지 않습니다."
                                    }
                                    """)
                    )
            )
    })
    @PatchMapping("/read-all")
    public ResponseEntity<Void> readAllNotifications(
            @AuthenticationPrincipal(expression = "userId") Long userId
    );

}
