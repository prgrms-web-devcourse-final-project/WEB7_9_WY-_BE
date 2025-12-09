package back.kalender.domain.performance.controller;

import back.kalender.domain.performance.dto.response.PerformanceDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
    // TODO : 서비스 주입

    @Operation(
            summary = "공연 상세 정보 조회",
            description = "특정 공연의 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PerformanceDetailResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "공연을 찾을 수 없음", content = @Content())
    })
    @GetMapping("/{performanceId}")
    public ResponseEntity<PerformanceDetailResponse> getPerformanceDetail(
            @Parameter(description = "공연 ID") @PathVariable Long performanceId
    ){
        // TODO: 실제 서비스 로직으로 교체
        PerformanceDetailResponse dummyData = new PerformanceDetailResponse(
                1L,
                "임영웅 IM HERO TOUR 2025 - 광주",
                "https://example.com/poster.jpg",
                new PerformanceDetailResponse.ArtistInfo(10L, "임영웅"),
                LocalDate.of(2025, 12, 19),
                LocalDate.of(2025, 12, 21),
                150,
                new PerformanceDetailResponse.PerformanceHallInfo(
                        5L,
                        "김대중컨벤션센터",
                        "광주광역시 서구 내방로 111",
                        "지하철 1호선 김대중컨벤션센터역 3번 출구"
                ),
                List.of(
                        new PerformanceDetailResponse.PriceGradeInfo(1L, "LOVE석", 176000),
                        new PerformanceDetailResponse.PriceGradeInfo(2L, "PEACE석", 154000)
                ),
                LocalDateTime.of(2025, 9, 23, 20, 0),
                LocalDateTime.of(2025, 12, 21, 17, 0),
                "※ 11/24(월)~28(금) 일괄배송 예정입니다."
        );

        return ResponseEntity.ok(dummyData);
    }
}
