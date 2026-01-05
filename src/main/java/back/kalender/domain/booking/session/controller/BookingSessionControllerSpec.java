package back.kalender.domain.booking.session.controller;

import back.kalender.domain.booking.session.dto.request.BookingSessionCreateRequest;
import back.kalender.domain.booking.session.dto.response.BookingSessionCreateResponse;
import back.kalender.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "BookingSession", description = "예매 세션 관리 API")
public interface BookingSessionControllerSpec {

    @Operation(
            summary = "BookingSession 생성",
            description = """
                    대기열 통과(ADMITTED) 후 예매창 진입 권한을 획득합니다.
                    
                    **핵심 정책**
                    - waitingToken은 1회용 (사용 후 즉시 소비됨)
                    - 대기열 진입 기기 = 예매 기기 (deviceId 검증)
                    - 1인 1기기 1세션 (중복 세션 생성 불가)
                    - 생성된 bookingSessionId로 모든 예매 API 호출
                    
                    **호출 시점**
                    - 대기열 상태 조회에서 status="ADMITTED" 받은 직후
                    - 좌석 배치도 화면 진입 전
                    
                    **생성되는 Redis 키**
                    - booking:session:{sessionId} → scheduleId (30분 TTL)
                    - booking:session:device:{sessionId} → deviceId (30분 TTL)
                    - booking:session:{userId}:{scheduleId} → sessionId (30분 TTL)
                    - active:{scheduleId} ZSET에 bookingSessionId 추가
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "BookingSession 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BookingSessionCreateResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "bookingSessionId": "bs_abc123def456"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "waitingToken 무효",
                                            value = """
                                            {
                                              "error": {
                                                "code": "INVALID_WAITING_TOKEN",
                                                "message": "유효하지 않은 대기열 토큰입니다."
                                              }
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "scheduleId 불일치",
                                            value = """
                                            {
                                              "error": {
                                                "code": "SCHEDULE_MISMATCH",
                                                "message": "대기열 토큰의 회차 정보가 일치하지 않습니다."
                                              }
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "qsid 만료",
                                            value = """
                                            {
                                              "error": {
                                                "code": "QSID_EXPIRED",
                                                "message": "대기열 세션이 만료되었습니다."
                                              }
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "deviceId 불일치",
                                            value = """
                                            {
                                              "error": {
                                                "code": "DEVICE_ID_MISMATCH",
                                                "message": "대기열 진입 기기와 예매 기기가 다릅니다."
                                              }
                                            }
                                            """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "error": {
                                        "code": "UNAUTHORIZED",
                                        "message": "인증이 필요합니다."
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 다른 기기로 세션 생성됨",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "error": {
                                        "code": "DEVICE_ALREADY_USED",
                                        "message": "이미 다른 기기로 예매 진행 중입니다."
                                      }
                                    }
                                    """
                            )
                    )
            )
    })
    @PostMapping("/create")
    ResponseEntity<BookingSessionCreateResponse> create(
            @Parameter(
                    description = "BookingSession 생성 요청",
                    required = true
            )
            @Valid @RequestBody BookingSessionCreateRequest request,

            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "Ping (Active 유지)",
            description = """
                    Active 상태를 유지하기 위한 heartbeat API입니다.
                    
                    **호출 주기**
                    - 10초마다 호출 (권장)
                    - 60초 이상 호출 없으면 ActiveSweepScheduler에 의해 자동 제거
                    
                    **호출 시점**
                    - 좌석 배치도 화면 진입 후
                    - 좌석 선택 중
                    - 예매 요약 화면
                    - 배송지 입력 화면
                    
                    **중단 시점**
                    - 결제 화면 진입 (leave API 호출)
                    - 예매 취소
                    - 브라우저 탭 닫힘
                    
                    **내부 동작**
                    - active:{scheduleId} ZSET의 score(timestamp) 갱신
                    - Active 상태 검증 (없으면 에러)
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Ping 성공 (Active 유지됨)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Active 상태가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "error": {
                                        "code": "NOT_IN_ACTIVE",
                                        "message": "Active 상태가 아닙니다."
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "BookingSession 만료",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "error": {
                                        "code": "BOOKING_SESSION_EXPIRED",
                                        "message": "예매 세션이 만료되었습니다."
                                      }
                                    }
                                    """
                            )
                    )
            )
    })
    @PostMapping("/ping/{scheduleId}")
    ResponseEntity<Void> ping(
            @Parameter(
                    description = "공연 회차 ID",
                    required = true,
                    example = "1"
            )
            @PathVariable Long scheduleId,

            @Parameter(
                    description = "예매 세션 ID",
                    required = true,
                    example = "bs_abc123def456"
            )
            @RequestHeader("X-BOOKING-SESSION-ID") String bookingSessionId
    );

    @Operation(
            summary = "Active 이탈 (Ping 중단)",
            description = """
                    Active 상태에서 명시적으로 이탈합니다.
                    
                    **호출 시점**
                    - 결제 화면 진입 (권장)
                    - 예매 취소
                    - 페이지 이탈
                    - 브라우저 탭 닫힘
                    
                    **효과**
                    - active:{scheduleId} ZSET에서 즉시 제거
                    - 다음 대기자가 바로 입장 가능
                    - Ping 중단해도 60초 후 자동 제거되지만, leave 호출 시 즉시 제거
                    
                    **호출 안 해도 괜찮은가?**
                    - 괜찮음 (ActiveSweepScheduler가 자동 정리)
                    - 하지만 호출하면 다음 사람이 더 빨리 입장 가능 (UX 개선)
                    
                    **내부 동작**
                    - active:{scheduleId} ZSET에서 bookingSessionId 제거
                    - 에러 발생 안 함 (이미 없어도 정상 처리)
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Active 이탈 성공"
            )
    })
    @PostMapping("/leave/{scheduleId}")
    ResponseEntity<Void> leave(
            @Parameter(
                    description = "공연 회차 ID",
                    required = true,
                    example = "1"
            )
            @PathVariable Long scheduleId,

            @Parameter(
                    description = "예매 세션 ID",
                    required = true,
                    example = "bs_abc123def456"
            )
            @RequestHeader("X-BOOKING-SESSION-ID") String bookingSessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}






















