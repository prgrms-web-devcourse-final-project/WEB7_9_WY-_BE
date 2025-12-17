package back.kalender.domain.booking.performanceSeat.controller;

import back.kalender.domain.booking.performanceSeat.dto.PerformanceSeatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@Tag(name = "Performance Seat", description = "좌석 조회 API")
public interface PerformanceSeatControllerSpec {

    @Operation(
        summary = "좌석 조회",
        description = """
        대기열을 통과한 사용자(active)만 좌석 정보를 조회할 수 있습니다.

        ⚠️ 이 API는 프론트엔드에서 폴링 방식으로 자동 호출됩니다.
        (사용자 직접 호출 아님)
        """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "좌석 목록 조회 성공"),
        @ApiResponse(responseCode = "429", description = "대기열 미통과"),
    })
    @GetMapping("/schedules/{scheduleId}")
    List<PerformanceSeatResponse> getPerformanceSeats(
        @Parameter(description = "공연 회차 ID", example = "1")
        @PathVariable Long scheduleId,

        @Parameter(
            description = "기기 식별자 (대기열/active 판별용)",
            example = "device-abc-123"
        )
        @RequestHeader("X-Device-Id") String deviceId
    );
}
