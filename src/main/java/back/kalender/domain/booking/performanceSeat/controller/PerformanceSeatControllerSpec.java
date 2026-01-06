package back.kalender.domain.booking.performanceSeat.controller;

import back.kalender.domain.booking.performanceSeat.dto.BlockSummaryResponse;
import back.kalender.domain.booking.performanceSeat.dto.SubBlockSummaryResponse;
import back.kalender.domain.booking.performanceSeat.dto.SeatDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@Tag(name = "Performance Seat (Query)", description = "좌석 조회 (분리 API)")
public interface PerformanceSeatControllerSpec {

    // =========================
    // 블록 요약 조회
    // =========================
    @Operation(
            summary = "좌석 블록 요약 조회",
            description = """
        대기열을 통과한 사용자(active)만 좌석 블록 요약 정보를 조회할 수 있습니다.

        - 좌석 개별 정보는 포함되지 않습니다.
        - 공연장 전체 구조를 빠르게 파악하기 위한 API입니다.
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "블록 요약 조회 성공"),
            @ApiResponse(responseCode = "429", description = "대기열 미통과"),
    })
    @GetMapping("/api/v1/performances/{scheduleId}/seats/summary")
    List<BlockSummaryResponse> getSeatBlockSummary(
            @Parameter(description = "공연 회차 ID", example = "1")
            @PathVariable Long scheduleId,

            @Parameter(
                    description = "예매 세션 ID (대기열 통과 후 발급)",
                    example = "booking-session-abc-123"
            )
            @RequestHeader("X-BOOKING-SESSION-ID") String bookingSessionId
    );

    // =========================
    // 서브블록 요약 조회
    // =========================
    @Operation(
            summary = "좌석 서브블록 요약 조회",
            description = """
        선택한 블록 내 서브블록(A1, A2 등)의 좌석 현황을 조회합니다.

        - 좌석 개별 정보는 포함되지 않습니다.
        - 좌석 선택 이전 단계에서 사용됩니다.
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "서브블록 요약 조회 성공"),
            @ApiResponse(responseCode = "429", description = "대기열 미통과"),
    })
    @GetMapping("/api/v1/performances/{scheduleId}/seats/blocks/{block}/sub-blocks")
    List<SubBlockSummaryResponse> getSeatSubBlockSummary(
            @Parameter(description = "공연 회차 ID", example = "1")
            @PathVariable Long scheduleId,

            @Parameter(description = "좌석 블록", example = "A")
            @PathVariable String block,

            @Parameter(
                    description = "예매 세션 ID (대기열 통과 후 발급)",
                    example = "booking-session-abc-123"
            )
            @RequestHeader("X-BOOKING-SESSION-ID") String bookingSessionId
    );

    // =========================
    // 좌석 상세 조회
    // =========================
    @Operation(
            summary = "좌석 상세 조회",
            description = """
        선택한 서브블록 내 실제 좌석 목록을 조회합니다.

        - 좌석 선택 UI에서 사용됩니다.
        - row / seat 번호 및 가격 등급 정보만 반환합니다.
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좌석 상세 조회 성공"),
            @ApiResponse(responseCode = "429", description = "대기열 미통과"),
    })
    @GetMapping("/api/v1/performances/{scheduleId}/seats/blocks/{block}/sub-blocks/{subBlock}")
    List<SeatDetailResponse> getSeatDetails(
            @Parameter(description = "공연 회차 ID", example = "1")
            @PathVariable Long scheduleId,

            @Parameter(description = "좌석 블록", example = "A")
            @PathVariable String block,

            @Parameter(description = "좌석 서브블록", example = "A1")
            @PathVariable String subBlock,

            @Parameter(
                    description = "예매 세션 ID (대기열 통과 후 발급)",
                    example = "booking-session-abc-123"
            )
            @RequestHeader("X-BOOKING-SESSION-ID") String bookingSessionId
    );
}
