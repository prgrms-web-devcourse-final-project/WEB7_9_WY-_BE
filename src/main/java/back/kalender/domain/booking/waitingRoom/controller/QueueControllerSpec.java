package back.kalender.domain.booking.waitingRoom.controller;

import back.kalender.domain.booking.waitingRoom.dto.QueueJoinResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Queue", description = "대기열 API")
public interface QueueControllerSpec {

    @Operation(
        summary = "대기열 진입",
        description = """
        공연 좌석 예매를 위한 대기열에 진입합니다.
        동일 deviceId는 멱등하게 처리됩니다.
        """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "대기열 진입 성공")
    })
    @PostMapping("/join/{scheduleId}")
    ResponseEntity<QueueJoinResponse> join(
        @PathVariable Long scheduleId,
        @RequestHeader("X-Device-Id") String deviceId
    );
}