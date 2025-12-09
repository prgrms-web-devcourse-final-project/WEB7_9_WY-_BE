package back.kalender.domain.schedule.controller;

import back.kalender.domain.schedule.dto.response.UpcomingEventsResponse;
import back.kalender.domain.schedule.dto.response.DailySchedulesResponse;
import back.kalender.domain.schedule.dto.response.MonthlySchedulesResponse;
import back.kalender.domain.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @GetMapping("/following")
    public ResponseEntity<MonthlySchedulesResponse> getFollowingSchedules(
            @RequestParam int year,
            @RequestParam int month
    ) {
        Long userId = 1L; // 임시 userId

        MonthlySchedulesResponse response = scheduleService.getFollowingSchedules(userId, year, month);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "개별 아티스트 월별 일정 조회",
            description = "특정 아티스트를 필터링하여 해당 아티스트의 월별 일정만 표시합니다."
    )
    @GetMapping("/artist/{artistId}")
    public ResponseEntity<MonthlySchedulesResponse> getArtistSchedules(
            @PathVariable Long artistId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        Long userId = 1L; // 임시 userId

        MonthlySchedulesResponse response = scheduleService.getArtistSchedules(userId, artistId, year, month);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "특정 날짜 상세 일정 조회",
            description = "캘린더에서 특정 날짜를 클릭했을 때, 해당 날짜의 상세 일정 목록을 팝업 형태로 제공합니다."
    )
    @GetMapping("/daily")
    public ResponseEntity<DailySchedulesResponse> getDailySchedules(
            @RequestParam String date,
            @RequestParam(required = false) Optional<Long> artistId
    ) {
        Long userId = 1L; // 임시 userId

        DailySchedulesResponse response = scheduleService.getDailySchedules(userId, date, artistId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "다가오는 일정 조회",
            description = "팔로우한 아티스트들의 다가오는 일정을 시간순으로 정렬하여 제공합니다."
    )
    @GetMapping("/upcoming")
    public ResponseEntity<UpcomingEventsResponse> getUpcomingSchedules(){
        Long userId = 1L; // 임시 userId

        UpcomingEventsResponse response = scheduleService.getUpcomingEvents(userId);
        return ResponseEntity.ok(response);
    }
}
