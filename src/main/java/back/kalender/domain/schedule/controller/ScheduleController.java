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
            summary = "캘린더 화면 통합 데이터 조회",
            description = "특정 년/월의 전체 일정(혹은 특정 아티스트 필터링)과 다가오는 일정을 한 번에 반환합니다. " +
                    "프론트엔드에서 날짜별 필터링을 수행하여 상세 내용을 표시할 수 있습니다."
    )
    @GetMapping("/integrated")
    public ResponseEntity<IntegratedSchedulesListResponse> getIntegratedSchedules(
            @RequestParam int year,
            @RequestParam int month,
            @Parameter(description = "특정 아티스트만 보고 싶을 때 ID 전달 (없으면 전체 팔로우 아티스트)")
            @RequestParam(required = false) Optional<Long> artistId
    ) {
        Long userId = 1L; // TODO: 임시 userId (추후 SecurityContext 등에서 획득)

        IntegratedSchedulesListResponse response = scheduleService.getIntegratedSchedules(userId, year, month, artistId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "이벤트 선택 목록 조회",
            description = "사용자가 팔로우하는 아티스트들의 외부 행사 중 현재 시점부터 31일 이내의 일정만 조회합니다. (파티 생성용과 파티 목록 조회용 드롭다운 목록 제공)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = EventsListResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "events": [
                                        {
                                          "scheduleId": 120,
                                          "title": "[BTS] WORLD TOUR 2026"
                                        },
                                        {
                                          "scheduleId": 121,
                                          "title": "[NewJeans] 미니 3집 발매 팬사인회"
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (유저 인증 실패 등)",
                    content = @Content(examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "2001",
                                        "status": "403",
                                        "message": "인증에 실패했습니다." 
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
    @GetMapping("/partyList")
    public ResponseEntity<EventsListResponse> getEventListsSchedules() {
        Long userId = 1L; //TODO: 임시 userId

        EventsListResponse response = scheduleService.getEventLists(userId);
        return ResponseEntity.ok(response);
    }
}
