package back.kalender.domain.booking.waitingRoom.controller;

import back.kalender.domain.booking.waitingRoom.dto.QueueJoinResponse;
import back.kalender.domain.booking.waitingRoom.service.QueueService;
import back.kalender.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController implements QueueControllerSpec{

    private final QueueService queueService;

    @PostMapping("/join/{scheduleId}")
    public ResponseEntity<QueueJoinResponse> join(
            @PathVariable Long scheduleId,
            @RequestHeader("X-Device-Id") String deviceId
    ) {
        QueueJoinResponse response =
                queueService.join(scheduleId, deviceId);

        return ResponseEntity.ok(response);
    }
}
