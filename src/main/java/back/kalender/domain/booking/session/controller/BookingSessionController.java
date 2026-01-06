package back.kalender.domain.booking.session.controller;

import back.kalender.domain.booking.reservation.service.ReservationService;
import back.kalender.domain.booking.session.dto.request.BookingSessionCreateRequest;
import back.kalender.domain.booking.session.dto.response.BookingSessionCreateResponse;
import back.kalender.domain.booking.session.service.BookingSessionService;
import back.kalender.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/booking-session")
@RequiredArgsConstructor
public class BookingSessionController implements BookingSessionControllerSpec {
    private final BookingSessionService bookingSessionService;
    private final ReservationService reservationService;

    @PostMapping("/create")
    @Override
    public ResponseEntity<BookingSessionCreateResponse> create(
            @Valid @RequestBody BookingSessionCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String bookingSessionId = bookingSessionService.createWithWaitingToken(
                userDetails.getUserId(),
                request.scheduleId(),
                request.waitingToken(),
                request.deviceId()
        );

        return ResponseEntity.ok(
                new BookingSessionCreateResponse(bookingSessionId)
        );
    }

    @PostMapping("/ping/{scheduleId}")
    @Override
    public ResponseEntity<Void> ping(
            @PathVariable Long scheduleId,
            @RequestHeader("X-BOOKING-SESSION-ID") String bookingSessionId
    ) {
        bookingSessionService.ping(scheduleId, bookingSessionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/leave/{scheduleId}")
    @Override
    public ResponseEntity<Void> leave(
            @PathVariable Long scheduleId,
            @RequestHeader("X-BOOKING-SESSION-ID") String bookingSessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        reservationService.cancelActiveReservationIfExists(userId, scheduleId);
        bookingSessionService.leaveActive(scheduleId, bookingSessionId);

        return ResponseEntity.noContent().build();
    }
}
