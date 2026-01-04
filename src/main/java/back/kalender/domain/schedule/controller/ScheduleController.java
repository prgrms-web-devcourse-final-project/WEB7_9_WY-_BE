package back.kalender.domain.schedule.controller;

import back.kalender.domain.schedule.dto.response.*;
import back.kalender.domain.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
public class ScheduleController implements ScheduleControllerSpec {

    private final ScheduleService scheduleService;

    @GetMapping("/by-artists")
    public ResponseEntity<FollowingSchedulesListResponse> getSchedulesByArtists(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long artistId,
            @RequestParam List<Long> artistIds
    ) {
        FollowingSchedulesListResponse response =
                scheduleService.getSchedulesByArtistIds(year, month, artistIds, artistId);

        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/following")
    public ResponseEntity<FollowingSchedulesListResponse> getFollowingSchedules(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long artistId,
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        FollowingSchedulesListResponse response = scheduleService.getFollowingSchedules(userId, year, month, artistId);

        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/partyList")
    public ResponseEntity<EventsListResponse> getEventListsSchedules(
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        EventsListResponse response = scheduleService.getEventLists(userId);
        return ResponseEntity.ok(response);
    }


}
