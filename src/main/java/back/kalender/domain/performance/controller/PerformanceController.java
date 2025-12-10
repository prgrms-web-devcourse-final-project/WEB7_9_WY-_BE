package back.kalender.domain.performance.controller;

import back.kalender.domain.performance.dto.response.PerformanceDetailResponse;
import back.kalender.domain.performance.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Performance", description = "공연 관련 API")
@RestController
@RequestMapping("/api/v1/performance")
@RequiredArgsConstructor
public class PerformanceController {
    private final PerformanceService performanceService;

    @Operation(
            summary = "공연 상세 정보 조회",
            description = "특정 공연의 상세 정보를 조회합니다. 예매 가능한 날짜 목록과 회차 정보를 포함합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PerformanceDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "공연을 찾을 수 없음",
                                            value = """
                                                {
                                                  "error": {
                                                    "code": "PERFORMANCE_NOT_FOUND",
                                                    "status": "404",
                                                    "message": "공연을 찾을 수 없습니다."
                                                  }
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "공연장을 찾을 수 없음",
                                            value = """
                                                {
                                                  "error": {
                                                    "code": "PERFORMANCE_HALL_NOT_FOUND",
                                                    "status": "404",
                                                    "message": "공연장을 찾을 수 없습니다."
                                                  }
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "아티스트를 찾을 수 없음",
                                            value = """
                                                {
                                                  "error": {
                                                    "code": "ARTIST_NOT_FOUND",
                                                    "status": "404",
                                                    "message": "아티스트를 찾을 수 없습니다."
                                                  }
                                                }
                                                """
                                    )
                            }
                    )
            )
    })
    @GetMapping("/{performanceId}")
    public ResponseEntity<PerformanceDetailResponse> getPerformanceDetail(
            @Parameter(description = "공연 ID") @PathVariable Long performanceId
    ){
        PerformanceDetailResponse response = performanceService.getPerformanceDetail(performanceId);
        return ResponseEntity.ok(response);    }
}
