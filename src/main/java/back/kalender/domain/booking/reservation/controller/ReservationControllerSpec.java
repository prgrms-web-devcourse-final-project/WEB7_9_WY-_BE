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
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reservation", description = "예매 관련 API")
public interface ReservationControllerSpec {

    @Operation(
            summary = "예매 세션 생성",
            description = """
                    대기열 통과 후 예약 세션을 생성합니다.
                    - 대기열 토큰 검증 후 5분간 유효한 예매 세션 생성
                    - 생성 즉시 좌석 선택 가능 (PENDING 상태)
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "예매 세션 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateReservationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "error": {
                                        "code": "SCHEDULE_NOT_FOUND",
                                        "message": "회차를 찾을 수 없습니다."
                                      }
                                    }
                                    """
                            )
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
            )
    })
    @PostMapping("/schedule/{scheduleId}/reservation")
    ResponseEntity<CreateReservationResponse> createReservation(
            @Parameter(description = "공연 회차 ID") @PathVariable Long scheduleId,
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "좌석 선점 (Hold)",
            description = """
                    선택한 좌석을 HOLD 상태로 변경합니다. (5분간 유효)
                    
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
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = HoldSeatsResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "좌석 선점 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "이미 선점된 좌석",
                                            value = """
                                            {
                                              "error": {
                                                "code": "SEAT_ALREADY_HELD",
                                                "message": "이미 선점된 좌석이 포함되어 있습니다."
                                              }
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "좌석 충돌",
                                            value = """
                                            {
                                              "error": {
                                                "code": "SEAT_CONFLICT",
                                                "message": "좌석 충돌이 발생했습니다. 새로고침 후 다시 시도해주세요.",
                                                "refreshRequired": true,
                                                "conflicts": [
                                                  {
                                                    "performanceSeatId": 1,
                                                    "reason": "ALREADY_HELD"
                                                  }
                                                ]
                                              }
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "예매 상태 오류",
                                            value = """
                                            {
                                              "error": {
                                                "code": "INVALID_RESERVATION_STATUS",
                                                "message": "예매 상태가 올바르지 않습니다."
                                              }
                                            }
                                            """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (다른 사용자의 예매)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "error": {
                                        "code": "UNAUTHORIZED",
                                        "message": "권한이 없습니다."
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "예매를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "error": {
                                        "code": "RESERVATION_NOT_FOUND",
                                        "message": "예매를 찾을 수 없습니다."
                                      }
                                    }
                                    """
                            )
                    )
            )
    })
    @PostMapping("/reservation/{reservationId}/seats:hold")
    ResponseEntity<HoldSeatsResponse> holdSeats(
            @Parameter(description = "예매 ID") @PathVariable Long reservationId,
            @Valid @RequestBody HoldSeatsRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "좌석 선점 해제 (Release)",
            description = """
                    HOLD 상태의 모든 좌석을 해제합니다.
                    - 좌석 변경 시 사용
                    - 해제된 좌석은 즉시 다른 사용자가 선택 가능
                    - 총액도 자동으로 재계산
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "좌석 해제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ReleaseSeatsResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "좌석 해제 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "error": {
                                        "code": "INVALID_RESERVATION_STATUS",
                                        "message": "예매 상태가 올바르지 않습니다."
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "예매를 찾을 수 없음")
    })
    @PostMapping("/reservation/{reservationId}/seats:release")
    ResponseEntity<ReleaseSeatsResponse> releaseSeats(
            @Parameter(description = "예매 ID") @PathVariable Long reservationId,
            @Valid @RequestBody ReleaseSeatsRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "예매 요약 조회(step3 우측)",
            description = """
                    현재 예매의 예매 정보, 공연 정보, 선택 좌석 정보를 조회합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ReservationSummaryResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "예매를 찾을 수 없음")
    })
    @GetMapping("/reservation/{reservationId}/summary")
    ResponseEntity<ReservationSummaryResponse> getReservationSummary(
            @Parameter(description = "예매 ID") @PathVariable Long reservationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "배송/수령 정보 저장",
            description = """
                    HOLD 상태에서 배송/수령 정보를 입력합니다. (여러 번 수정 가능)
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "입력 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UpdateDeliveryInfoResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "입력 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "상태 오류",
                                            value = """
                                            {
                                              "error": {
                                                "code": "INVALID_RESERVATION_STATUS",
                                                "message": "예매 상태가 올바르지 않습니다."
                                              }
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "만료된 예매",
                                            value = """
                                            {
                                              "error": {
                                                "code": "RESERVATION_EXPIRED",
                                                "message": "예매 시간이 만료되었습니다."
                                              }
                                            }
                                            """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "예매를 찾을 수 없음")
    })
    @PutMapping("/reservation/{reservationId}/delivery")
    ResponseEntity<UpdateDeliveryInfoResponse> updateDeliveryInfo(
            @Parameter(description = "예매 ID") @PathVariable Long reservationId,
            @Valid @RequestBody UpdateDeliveryInfoRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "예매 취소",
            description = "결제 완료된 예매를 취소합니다. (공연 시작 1시간 전까지 가능)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "취소 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CancelReservationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "취소 불가",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "상태 오류",
                                            value = """
                                            {
                                              "error": {
                                                "code": "INVALID_RESERVATION_STATUS",
                                                "message": "예매 상태가 올바르지 않습니다."
                                              }
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "기한 경과",
                                            value = """
                                            {
                                              "error": {
                                                "code": "CANCEL_DEADLINE_PASSED",
                                                "message": "취소 가능 기한이 지났습니다. (공연 시작 1시간 전까지 취소 가능)"
                                              }
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "좌석 없음",
                                            value = """
                                            {
                                              "error": {
                                                "code": "NO_SEATS_RESERVED",
                                                "message": "예매된 좌석이 없습니다."
                                              }
                                            }
                                            """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "예매를 찾을 수 없음")
    })
    @DeleteMapping("/reservation/{reservationId}")
    ResponseEntity<CancelReservationResponse> cancelReservation(
            @Parameter(description = "예매 ID") @PathVariable Long reservationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "좌석 상태 변경 조회 (폴링)",
            description = "특정 회차의 좌석 상태 변경 이력을 조회합니다. 프론트엔드에서 주기적으로 호출하여 좌석표 UI를 업데이트합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = SeatChangesResponse.class))
            )
    })
    @GetMapping("/schedule/{scheduleId}/seats/changes")
    ResponseEntity<SeatChangesResponse> getSeatChanges(
            @Parameter(description = "회차 ID", required = true)
            @PathVariable Long scheduleId,

            @Parameter(description = "마지막으로 받은 버전 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") Long sinceVersion
    );

    @Operation(
            summary = "내 예매 내역 조회",
            description = "완료된 예매 내역(결제 완료, 취소)을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MyReservationListResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/my-reservations")
    ResponseEntity<MyReservationListResponse> getMyReservations(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "예매 상세 조회",
            description = "예매 정보, 공연 정보, 좌석 정보, 결제 내역, 배송 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ReservationDetailResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "예매를 찾을 수 없음")
    })
    @GetMapping("/reservation/{reservationId}")
    ResponseEntity<ReservationDetailResponse> getReservationDetail(
            @Parameter(description = "예매 ID", example = "1", required = true)
            @PathVariable Long reservationId,

            @AuthenticationPrincipal CustomUserDetails userDetails
    );

}
