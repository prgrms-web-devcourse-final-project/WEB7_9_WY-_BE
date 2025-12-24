package back.kalender.domain.booking.waitingRoom.controller;

import back.kalender.domain.booking.waitingRoom.dto.QueueJoinResponse;
import back.kalender.domain.booking.waitingRoom.dto.QueueStatusResponse;
import back.kalender.domain.booking.waitingRoom.service.QueueAccessService;
import back.kalender.domain.booking.waitingRoom.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController implements QueueControllerSpec {

    private final QueueService queueService;
    private final QueueAccessService queueAccessService;

    @PostMapping("/join/{scheduleId}")
    public ResponseEntity<QueueJoinResponse> join(
            @PathVariable Long scheduleId,
            @RequestHeader("X-Device-Id") String deviceId
    ) {
        QueueJoinResponse response = queueService.join(scheduleId, deviceId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{scheduleId}")
    public ResponseEntity<QueueStatusResponse> status(
            @PathVariable Long scheduleId,
            @RequestHeader("X-QSID") String qsid
    ) {
        return ResponseEntity.ok(queueService.status(scheduleId, qsid));
    }

    @PostMapping("/ping/{scheduleId}")
    public void ping(
            @PathVariable Long scheduleId,
            @RequestHeader("X-BOOKING-SESSION-ID") String bookingSessionId
    ) {
        queueAccessService.ping(scheduleId, bookingSessionId);
    }
}