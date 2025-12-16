//package back.kalender.domain.schedule.controller;
//
//import back.kalender.domain.schedule.dto.response.*;
//import back.kalender.domain.schedule.service.ScheduleService;
//import back.kalender.global.security.util.SecurityUtil;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.media.Content;
//import io.swagger.v3.oas.annotations.media.ExampleObject;
//import io.swagger.v3.oas.annotations.media.Schema;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Optional;
//
//@RestController
//@RequestMapping("/api/v1/schedule")
//@RequiredArgsConstructor
//@Tag(name = "스케쥴/캘린더 API", description = "아티스트의 일정 조회 및 필터링 기능 제공")
//public class ScheduleController {
//
//    private final ScheduleService scheduleService;
//
//    @Operation(
//            summary = "캘린더 화면 통합 데이터 조회",
//            description = "특정 년/월의 전체 일정(혹은 특정 아티스트 필터링)과 다가오는 일정을 한 번에 반환합니다. " +
//                    "프론트엔드에서 날짜별 필터링을 수행하여 상세 내용을 표시할 수 있습니다."
//    )
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "조회 성공",
//                    content = @Content(schema = @Schema(implementation = FollowingSchedulesListResponse.class),
//                            examples = @ExampleObject(value = """
//                                    {
//                                      "monthlySchedules": [
//                                        {
//                                          "scheduleId": 120,
//                                          "artistId": 1,
//                                          "artistName": "NewJeans",
//                                          "title": "뮤직뱅크 출연",
//                                          "scheduleCategory": "BROADCAST",
//                                          "scheduleTime": "2025-12-15T17:00:00",
//                                          "performanceId": null,
//                                          "link": null,
//                                          "location": "KBS 신관 공개홀"
//                                        },
//                                        {
//                                          "scheduleId": 121,
//                                          "artistId": 2,
//                                          "artistName": "BTS",
//                                          "title": "팬사인회",
//                                          "scheduleCategory": "FAN_SIGN",
//                                          "scheduleTime": "2025-12-20T19:00:00",
//                                          "performanceId": null,
//                                          "link": "https://ticket.example.com/bts",
//                                          "location": "코엑스"
//                                        }
//                                      ],
//                                      "upcomingEvents": [
//                                        {
//                                          "scheduleId": 205,
//                                          "artistName": "aespa",
//                                          "title": "미니 4집 발매 콘서트",
//                                          "scheduleCategory": "CONCERT",
//                                          "scheduleTime": "2025-12-25T18:00:00",
//                                          "performanceId": 501,
//                                          "link": "https://ticket.example.com/aespa",
//                                          "daysUntilEvent": 5,
//                                          "location": "고척 스카이돔"
//                                        }
//                                      ]
//                                    }
//                                    """))),
//            @ApiResponse(responseCode = "400", description = "잘못된 입력 (예: 월 범위 초과)",
//                    content = @Content(examples = @ExampleObject(value = """
//                                    {
//                                      "error": {
//                                        "code": "004",
//                                        "status": "400",
//                                        "message": "유효하지 않은 입력값입니다. (month는 1~12 사이여야 합니다)"
//                                      }
//                                    }
//                                    """))),
//            @ApiResponse(responseCode = "403", description = "권한 없음 (팔로우하지 않은 아티스트 필터링 시도)",
//                    content = @Content(examples = @ExampleObject(value = """
//                                    {
//                                      "error": {
//                                        "code": "2003",
//                                        "status": "403",
//                                        "message": "해당 아티스트를 팔로우하고 있지 않습니다."
//                                      }
//                                    }
//                                    """)))
//    })
//    @GetMapping("/following")
//    public ResponseEntity<FollowingSchedulesListResponse> getFollowingSchedules(
//            @RequestParam int year,
//            @RequestParam int month,
//            @Parameter(description = "특정 아티스트만 보고 싶을 때 ID 전달 (없으면 전체 팔로우 아티스트)")
//            @RequestParam(required = false) Optional<Long> artistId
//    ) {
//        Long userId = SecurityUtil.getCurrentUserIdOrThrow();
//
//        FollowingSchedulesListResponse response = scheduleService.getFollowingSchedules(userId, year, month, artistId);
//
//        return ResponseEntity.ok(response);
//    }
//
//    @Operation(
//            summary = "이벤트 선택 목록 조회",
//            description = "사용자가 팔로우하는 아티스트들의 외부 행사 중 현재 시점부터 31일 이내의 일정만 조회합니다. (파티 생성용과 파티 목록 조회용 드롭다운 목록 제공)"
//    )
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "조회 성공",
//                    content = @Content(schema = @Schema(implementation = EventsListResponse.class),
//                            examples = @ExampleObject(value = """
//                                    {
//                                      "events": [
//                                        {
//                                          "scheduleId": 120,
//                                          "title": "[BTS] WORLD TOUR 2026"
//                                        },
//                                        {
//                                          "scheduleId": 121,
//                                          "title": "[NewJeans] 미니 3집 발매 팬사인회"
//                                        }
//                                      ]
//                                    }
//                                    """))),
//            @ApiResponse(responseCode = "403", description = "권한 없음 (유저 인증 실패 등)",
//                    content = @Content(examples = @ExampleObject(value = """
//                                    {
//                                      "error": {
//                                        "code": "2001",
//                                        "status": "403",
//                                        "message": "인증에 실패했습니다."
//                                      }
//                                    }
//                                    """))),
//            @ApiResponse(responseCode = "404", description = "일정 조회 실패",
//                    content = @Content(examples = @ExampleObject(value = """
//                                    {
//                                      "error": {
//                                        "code": "4001",
//                                        "status": "404",
//                                        "message": "일정을 찾을 수 없습니다."
//                                      }
//                                    }
//                                    """)))
//    })
//    @GetMapping("/partyList")
//    public ResponseEntity<EventsListResponse> getEventListsSchedules() {
//        Long userId = SecurityUtil.getCurrentUserIdOrThrow();
//
//        EventsListResponse response = scheduleService.getEventLists(userId);
//        return ResponseEntity.ok(response);
//    }
//}
