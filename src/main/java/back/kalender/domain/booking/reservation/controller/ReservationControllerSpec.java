package back.kalender.domain.booking.reservation.controller;

import back.kalender.domain.booking.reservation.dto.request.CreateReservationRequest;
import back.kalender.domain.booking.reservation.dto.request.HoldSeatsRequest;
import back.kalender.domain.booking.reservation.dto.request.ReleaseSeatsRequest;
import back.kalender.domain.booking.reservation.dto.request.UpdateDeliveryInfoRequest;
import back.kalender.domain.booking.reservation.dto.response.*;
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
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Reservation", description = "예매 관련 API")
public interface ReservationControllerSpec {

    @Operation(
            summary = "예매 세션 생성",
            description = """
                    대기열 통과 후 예약 세션을 생성합니다.
                    - 대기열 토큰 검증 후 5분간 유효한 예매 세션 생성
                    - 생성 즉시 좌석 선택 가능 (상태 HOLD)
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "예매 세션 생성 성공",
                    content = @Content(schema = @Schema(implementation = CreateReservationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "INVALID_WAITING_TOKEN",
                                "status": "400",
                                "message": "유효하지 않은 대기열 토큰입니다."
                              }
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "UNAUTHORIZED",
                                "status": "401",
                                "message": "로그인이 필요합니다."
                              }
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "회차를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "SCHEDULE_NOT_FOUND",
                                "status": "404",
                                "message": "일정을 찾을 수 없습니다."
                              }
                            }
                            """))
            )
    })
    ResponseEntity<CreateReservationResponse> createReservation(
            @Parameter(description = "공연 회차 ID") @PathVariable Long scheduleId,
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "좌석 선점 (Hold)",
            description = """
                    선택한 좌석들을 예매 세션에 추가합니다.
                    **핵심 정책**
                    - 최대 4석까지 선택 가능
                    - 선택 좌석 중 하나라도 실패 시 전체 실패 (부분 성공 없음)
                    - Redis 분산 락으로 동시성 제어
                    - 첫 좌석 HOLD 성공 시점부터 TTL 카운트 시작
                    
                    **충돌 처리**
                    - 실패 시 409 CONFLICT 반환
                    - 응답에 충돌 좌석 목록 + 사유 포함
                    - 프론트는 좌석표 재조회 필요
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "좌석 선점 성공",
                    content = @Content(schema = @Schema(implementation = HoldSeatsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "좌석 선점 충돌 (이미 선점된 좌석 포함)",
                    content = @Content(
                            schema = @Schema(implementation = HoldSeatsFailResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "reservationId": 123,
                                      "refreshRequired": true,
                                      "conflicts": [
                                        {
                                          "performanceSeatId": 102,
                                          "currentStatus": "HOLD",
                                          "reason": "ALREADY_HELD"
                                        }
                                      ],
                                      "updatedAt": "2026-01-05T14:10:30"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(examples = {
                            @ExampleObject(name = "좌석 수 초과", value = """
                                    {
                                      "error": {
                                        "code": "SEAT_COUNT_EXCEEDED",
                                        "status": "400",
                                        "message": "최대 4석까지 선택 가능합니다."
                                      }
                                    }
                                    """),
                            @ExampleObject(name = "예매 만료", value = """
                                    {
                                      "error": {
                                        "code": "RESERVATION_EXPIRED",
                                        "status": "400",
                                        "message": "예매 시간이 만료되었습니다."
                                      }
                                    }
                                    """)
                    })
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "예매를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "RESERVATION_NOT_FOUND",
                                "status": "404",
                                "message": "예매를 찾을 수 없습니다."
                              }
                            }
                            """))
            ),
    })
    ResponseEntity<HoldSeatsResponse> holdSeats(
            @Parameter(description = "예매 ID") @PathVariable Long reservationId,
            @Valid @RequestBody HoldSeatsRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "좌석 선점 해제 (Release)",
            description = """
                    선택한 좌석 일부를 예매에서 제거합니다.
                    - 좌석 변경 시 사용
                    - 해제된 좌석은 즉시 다른 사용자가 선택 가능
                    - 총액도 자동으로 재계산
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "좌석 해제 성공",
                    content = @Content(schema = @Schema(implementation = ReleaseSeatsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "SEAT_NOT_HELD",
                                "status": "400",
                                "message": "예매에 포함되지 않은 좌석입니다."
                              }
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "예매를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "RESERVATION_NOT_FOUND",
                                "status": "404",
                                "message": "예매를 찾을 수 없습니다."
                              }
                            }
                            """))
            )
    })
    ResponseEntity<ReleaseSeatsResponse> releaseSeats(
            @Parameter(description = "예매 ID") @PathVariable Long reservationId,
            @Valid @RequestBody ReleaseSeatsRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "예매 요약 조회(step3 우측)",
            description = """
                    현재 예매의 전체 정보를 조회합니다.
                    - 공연 정보, 회차 정보
                    - 선택된 좌석 목록
                    - 총 금액, 남은 시간
                    - 취소 가능 기한
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ReservationSummaryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "UNAUTHORIZED_RESERVATION_ACCESS",
                                "status": "403",
                                "message": "예매에 접근할 권한이 없습니다."
                              }
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "예매를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "RESERVATION_NOT_FOUND",
                                "status": "404",
                                "message": "예매를 찾을 수 없습니다."
                              }
                            }
                            """))
            )
    })
    ResponseEntity<ReservationSummaryResponse> getReservationSummary(
            @Parameter(description = "예매 ID") @PathVariable Long reservationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "배송/수령 정보 저장",
            description = """
                    배송 방법과 수령인 정보를 저장합니다.
                    - DELIVERY: 배송지 정보 필수
                    - PICKUP: 이름, 전화번호만 필수
                    - 마이페이지 정보 자동 불러오기 가능
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "저장 성공",
                    content = @Content(schema = @Schema(implementation = UpdateDeliveryInfoResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(examples = {
                            @ExampleObject(name = "배송지 미입력", value = """
                                    {
                                      "error": {
                                        "code": "DELIVERY_ADDRESS_REQUIRED",
                                        "status": "400",
                                        "message": "배송지 정보를 입력해주세요."
                                      }
                                    }
                                    """),
                            @ExampleObject(name = "예매 만료", value = """
                                    {
                                      "error": {
                                        "code": "RESERVATION_EXPIRED",
                                        "status": "400",
                                        "message": "예매 시간이 만료되었습니다."
                                      }
                                    }
                                    """)
                    })
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "예매를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "RESERVATION_NOT_FOUND",
                                "status": "404",
                                "message": "예매를 찾을 수 없습니다."
                              }
                            }
                            """))
            )
    })
    ResponseEntity<UpdateDeliveryInfoResponse> updateDeliveryInfo(
            @Parameter(description = "예매 ID") @PathVariable Long reservationId,
            @Valid @RequestBody UpdateDeliveryInfoRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "예매 취소",
            description = """
                    예매 세션을 취소하고 선택한 모든 좌석을 해제합니다.
                    - 모든 hold된 좌석 즉시 해제
                    - 예매 상태 CANCELLED로 변경
                    - 결제 완료 후에는 취소 불가
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "취소 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "CANNOT_CANCEL_PAID_RESERVATION",
                                "status": "400",
                                "message": "결제 완료된 예매는 취소할 수 없습니다."
                              }
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "예매를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "RESERVATION_NOT_FOUND",
                                "status": "404",
                                "message": "예매를 찾을 수 없습니다."
                              }
                            }
                            """))
            )
    })
    ResponseEntity<Void> cancelReservation(
            @Parameter(description = "예매 ID") @PathVariable Long reservationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
