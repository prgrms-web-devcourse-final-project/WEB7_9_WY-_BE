package back.kalender.domain.booking.reservation.controller;

import back.kalender.domain.booking.reservation.dto.request.CreateReservationRequest;
import back.kalender.domain.booking.reservation.dto.request.HoldSeatsRequest;
import back.kalender.domain.booking.reservation.dto.request.ReleaseSeatsRequest;
import back.kalender.domain.booking.reservation.dto.request.UpdateDeliveryInfoRequest;
import back.kalender.domain.booking.reservation.dto.response.*;
import back.kalender.domain.booking.reservation.service.ReservationService;
import back.kalender.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/booking")
@RequiredArgsConstructor
public class ReservationController implements ReservationControllerSpec {

    private final ReservationService reservationService;

    @PostMapping("/schedule/{scheduleId}/reservation")
    @Override
    public ResponseEntity<CreateReservationResponse> createReservation(
            @PathVariable Long scheduleId,
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CreateReservationResponse response = reservationService.createReservation(
                scheduleId,
                request,
                userDetails.getUserId()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reservations/{reservationId}/seats:hold")
    @Override
    public ResponseEntity<HoldSeatsResponse> holdSeats(
            @PathVariable Long reservationId,
            @Valid @RequestBody HoldSeatsRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        HoldSeatsResponse response = reservationService.holdSeats(
                reservationId,
                request,
                userDetails.getUserId()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reservations/{reservationId}/seats:release")
    @Override
    public ResponseEntity<ReleaseSeatsResponse> releaseSeats(
            @PathVariable Long reservationId,
            @Valid @RequestBody ReleaseSeatsRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ReleaseSeatsResponse response = reservationService.releaseSeats(
                reservationId,
                request,
                userDetails.getUserId()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reservations/{reservationId}/summary")
    @Override
    public ResponseEntity<ReservationSummaryResponse> getReservationSummary(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ReservationSummaryResponse response = reservationService.getReservationSummary(
                reservationId,
                userDetails.getUserId()
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/reservations/{reservationId}/delivery")
    @Override
    public ResponseEntity<UpdateDeliveryInfoResponse> updateDeliveryInfo(
            @PathVariable Long reservationId,
            @Valid @RequestBody UpdateDeliveryInfoRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UpdateDeliveryInfoResponse response = reservationService.updateDeliveryInfo(
                reservationId,
                request,
                userDetails.getUserId()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reservations/{reservationId}")
    @Override
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        reservationService.cancelReservation(
                reservationId,
                userDetails.getUserId()
        );
        return ResponseEntity.noContent().build();
    }
}
