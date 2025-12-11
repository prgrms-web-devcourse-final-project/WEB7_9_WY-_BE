package back.kalender.domain.schedule.controller;

import back.kalender.domain.schedule.dto.response.*;
import back.kalender.domain.schedule.service.ScheduleService;
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
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MonthlySchedulesListResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "schedules": [
                                        {
                                          "id": 101,
                                          "artistId": 2,
                                          "artistName": "BTS",
                                          "title": "콘서트 - BTS",
                                          "scheduleCategory": "CONCERT",
                                          "scheduleTime": "2025-11-02T19:00:00",
                                          "performanceId": 501,
                                          "date": "2025-11-02",
                                          "location": "고척 스카이돔"
                                        },
                                        {
                                          "id": 102,
                                          "artistId": 3,
                                          "artistName": "NewJeans",
                                          "title": "팬미팅 - NewJeans",
                                          "scheduleCategory": "FAN_MEETING",
                                          "scheduleTime": "2025-11-04T14:00:00",
                                          "performanceId": null,
                                          "date": "2025-11-04",
                                          "location": null
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (날짜 형식 오류 등)",
                    content = @Content(examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "002",
                                        "status": "400",
                                        "message": "유효하지 않은 입력값입니다."
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "일정 조회 실패",
                    content = @Content(examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "4001",
                                        "status": "404",
                                        "message": "일정을 찾을 수 없습니다."
                                      }
                                    }
                                    """)))
    })
    @GetMapping("/following")
    public ResponseEntity<MonthlySchedulesListResponse> getFollowingSchedules(
            @RequestParam int year,
            @RequestParam int month
    ) {
        Long userId = 1L; //TODO: 임시 userId

        MonthlySchedulesListResponse response = scheduleService.getFollowingSchedules(userId, year, month);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "개별 아티스트 월별 일정 조회",
            description = "특정 아티스트를 필터링하여 해당 아티스트의 월별 일정만 표시합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MonthlySchedulesListResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "002",
                                        "status": "400",
                                        "message": "유효하지 않은 입력 값입니다."
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (팔로우하지 않음)",
                    content = @Content(examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "2001",
                                        "status": "403",
                                        "message": "팔로우하지 않은 아티스트입니다."
                                      }
                                    }
                                    """)))
    })
    @GetMapping("/artist/{artistId}")
    public ResponseEntity<MonthlySchedulesListResponse> getSchedulesPerArtist(
            @PathVariable Long artistId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        Long userId = 1L; //TODO: 임시 userId

        MonthlySchedulesListResponse response = scheduleService.getSchedulesPerArtist(userId, artistId, year, month);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "특정 날짜 상세 일정 조회",
            description = "캘린더에서 특정 날짜를 클릭했을 때, 해당 날짜의 상세 일정 목록을 팝업 형태로 제공합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = DailySchedulesListResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "dailySchedules": [
                                        {
                                          "scheduleId": 120,
                                          "artistName": "NewJeans",
                                          "title": "뮤직뱅크 출연",
                                          "scheduleCategory": "BROADCAST",
                                          "scheduleTime": "2025-12-15T17:00:00",
                                          "performanceId": null,
                                          "link": null,
                                          "location": "KBS 신관 공개홀"
                                        },
                                        {
                                          "scheduleId": 121,
                                          "artistName": "BTS",
                                          "title": "팬사인회",
                                          "scheduleCategory": "FAN_SIGN",
                                          "scheduleTime": "2025-12-15T19:00:00",
                                          "performanceId": null,
                                          "link": "https://ticket.example.com/bts",
                                          "location": "코엑스"
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (날짜 형식 오류)",
                    content = @Content(examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "002",
                                        "status": "400",
                                        "message": "유효하지 않은 입력 값입니다. (Date Format: yyyy-MM-dd)"
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (팔로우하지 않음)",
                    content = @Content(examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "2001",
                                        "status": "403",
                                        "message": "팔로우하지 않은 아티스트입니다."
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "스케쥴 조회 실패 (유저 정보 없음)",
                    content = @Content(examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "1001",
                                        "status": "404",
                                        "message": "유저를 찾을 수 없습니다."
                                      }
                                    }
                                    """)))
    })
    @GetMapping("/daily")
    public ResponseEntity<DailySchedulesListResponse> getDailySchedules(
            @RequestParam String date,
            @RequestParam(required = false) Optional<Long> artistId
    ) {
        Long userId = 1L; //TODO: 임시 userId

        DailySchedulesListResponse response = scheduleService.getDailySchedules(userId, date, artistId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "다가오는 일정 조회",
            description = "팔로우한 아티스트들의 다가오는 일정을 시간순으로 정렬하여 제공합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UpcomingEventsListResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "upcomingEvents": [
                                        {
                                          "scheduleId": 205,
                                          "artistName": "aespa",
                                          "title": "팬사인회",
                                          "scheduleCategory": "FAN_SIGN",
                                          "scheduleTime": "2025-12-20T14:00:00",
                                          "performanceId": null,
                                          "link": "https://example.com",
                                          "daysUntilEvent": 5,
                                          "location": "코엑스"
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (Limit 값 오류)",
                    content = @Content(examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "002",
                                        "status": "400",
                                        "message": "유효하지 않은 입력 값입니다."
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (팔로우하지 않음)",
                    content = @Content(examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "2001",
                                        "status": "403",
                                        "message": "팔로우하지 않은 아티스트입니다."
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "스케쥴 조회 실패 (유저 정보 없음)",
                    content = @Content(examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "1001",
                                        "status": "404",
                                        "message": "유저를 찾을 수 없습니다."
                                      }
                                    }
                                    """)))
    })
    @GetMapping("/upcoming")
    public ResponseEntity<UpcomingEventsListResponse> getUpcomingSchedules(
            @RequestParam(required = false) Optional<Long> artistId,
            @Parameter(description = "가져올 일정 개수 (기본값 10)", example = "5")
            @RequestParam(required = false, defaultValue = "10") int limit
    ){
        Long userId = 1L; //TODO: 임시 userId

        UpcomingEventsListResponse response = scheduleService.getUpcomingEvents(userId, artistId, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/partyList")
    public ResponseEntity<EventsListResponse> getEventListsSchedules(
            @RequestParam(required = false) Optional<Long> artistId
    ) {
        Long userId = 1L; //TODO: 임시 userId

        EventsListResponse response = scheduleService.getEventLists(userId, artistId);
        return ResponseEntity.ok(response);
    }
}
