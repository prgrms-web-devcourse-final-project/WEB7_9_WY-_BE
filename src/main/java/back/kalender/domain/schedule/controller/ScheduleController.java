package back.kalender.domain.schedule.controller;

import back.kalender.domain.schedule.dto.response.DailyScheduleItem;
import back.kalender.domain.schedule.dto.response.DailySchedulesResponse;
import back.kalender.domain.schedule.dto.response.MonthlySchedulesResponse;
import back.kalender.domain.schedule.dto.response.ScheduleItem;
import back.kalender.domain.schedule.entity.ScheduleCategory;
import back.kalender.domain.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
@Tag(name = "스케쥴/캘린더 API", description = "아티스트의 일정 조회 및 필터링 기능 제공")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(
            summary = "월별 팔로우 전체 일정 조회",
            description = "팔로우한 모든 아티스트의 특정 월 일정 데이터를 가져와 캘린더에 표시합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류")
    })
    @GetMapping("/following")
    public ResponseEntity<MonthlySchedulesResponse> getFollowingSchedules(
            @RequestParam int year,
            @RequestParam int month
    ) {
        List<ScheduleItem> dummyList = List.of(
                new ScheduleItem(101L, 2L, "BTS", "콘서트 - BTS", ScheduleCategory.CONCERT, Optional.of(501L), LocalDateTime.of(year, month, 2, 19, 0), LocalDate.of(year, month, 2)),
                new ScheduleItem(105L, 4L, "aespa", "뮤직뱅크 - aespa", ScheduleCategory.BROADCAST, Optional.empty(), LocalDateTime.of(year, month, 10, 17, 0), LocalDate.of(year, month, 10)),
                new ScheduleItem(106L, 6L, "IVE", "팬미팅 - IVE", ScheduleCategory.FAN_MEETING, Optional.of(601L), LocalDateTime.of(year, month, 10, 14, 0), LocalDate.of(year, month, 10)),
                new ScheduleItem(108L, 2L, "BTS", "지민 생일", ScheduleCategory.BIRTHDAY, Optional.empty(), LocalDateTime.of(year, month, 18, 0, 0), LocalDate.of(year, month, 18))
        );
        MonthlySchedulesResponse response = new MonthlySchedulesResponse(dummyList);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "개별 아티스트 월별 일정 조회",
            description = "특정 아티스트를 필터링하여 해당 아티스트의 월별 일정만 표시합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "팔로우하지 않은 아티스트"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류")
    })
    @GetMapping("/artist/{artistId}")
    public ResponseEntity<MonthlySchedulesResponse> getArtistSchedules(
            @PathVariable Long artistId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        List<ScheduleItem> dummyList = List.of(
                new ScheduleItem(105L, artistId, "aespa", "뮤직뱅크", ScheduleCategory.BROADCAST, Optional.empty(), LocalDateTime.of(year, month, 10, 17, 0), LocalDate.of(year, month, 10)),
                new ScheduleItem(109L, artistId, "aespa", "팬사인회 응모", ScheduleCategory.FAN_SIGN, Optional.empty(), LocalDateTime.of(year, month, 25, 20, 0), LocalDate.of(year, month, 25))
        );
        MonthlySchedulesResponse response = new MonthlySchedulesResponse(dummyList);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "특정 날짜 상세 일정 조회",
            description = "캘린더에서 특정 날짜를 클릭했을 때, 해당 날짜의 상세 일정 목록을 팝업 형태로 제공합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "날짜 형식 오류")
    })
    @GetMapping("/daily")
    public ResponseEntity<DailySchedulesResponse> getDailySchedules(
            @RequestParam String date,
            @RequestParam(required = false) Optional<Long> artistId
    ) {
        List<DailyScheduleItem> dummyList = List.of(
                new DailyScheduleItem(105L, "aespa", "뮤직뱅크 - aespa", ScheduleCategory.BROADCAST, LocalDateTime.of(2025, 11, 10, 17, 0), Optional.of("https://kbs.co.kr/apply"), Optional.empty()),
                new DailyScheduleItem(106L, "IVE", "팬미팅 - IVE", ScheduleCategory.FAN_MEETING, LocalDateTime.of(2025, 11, 10, 14, 0), Optional.of("https://ticket.yes24.com/ive"), Optional.of(601L))
        );
        DailySchedulesResponse response = new DailySchedulesResponse(dummyList);

        return ResponseEntity.ok(response);
    }
}
