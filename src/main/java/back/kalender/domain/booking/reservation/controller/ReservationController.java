package back.kalender.domain.booking.reservation.controller;

import back.kalender.domain.booking.reservation.dto.request.CreateReservationRequest;
import back.kalender.domain.booking.reservation.dto.request.HoldSeatsRequest;
import back.kalender.domain.booking.reservation.dto.request.ReleaseSeatsRequest;
import back.kalender.domain.booking.reservation.dto.request.UpdateDeliveryInfoRequest;
import back.kalender.domain.booking.reservation.dto.response.*;
import back.kalender.domain.booking.reservation.service.ReservationService;
import back.kalender.domain.booking.session.service.BookingSessionService;
import back.kalender.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
            @RequestHeader("X-BOOKING-SESSION-ID") String bookingSessionId,
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CreateReservationResponse response = reservationService.createReservation(
                scheduleId,
                request,
                userDetails.getUserId(),
                bookingSessionId
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reservation/{reservationId}/seats:hold")
    @Override
    public ResponseEntity<HoldSeatsResponse> holdSeats(
            @PathVariable Long reservationId,
            @RequestHeader("X-BOOKING-SESSION-ID") String bookingSessionId,
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

    @PostMapping("/reservation/{reservationId}/seats:release")
    @Override
    public ResponseEntity<ReleaseSeatsResponse> releaseSeats(
            @PathVariable Long reservationId,
            @Valid @RequestBody ReleaseSeatsRequest request,
            @RequestHeader("X-BOOKING-SESSION-ID") String bookingSessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        ReleaseSeatsResponse response = reservationService.releaseSeats(
                reservationId,
                request,
                userDetails.getUserId()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reservation/{reservationId}/summary")
    @Override
    public ResponseEntity<ReservationSummaryResponse> getReservationSummary(
            @PathVariable Long reservationId,
            @RequestHeader("X-BOOKING-SESSION-ID") String bookingSessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ReservationSummaryResponse response = reservationService.getReservationSummary(
                reservationId,
                userDetails.getUserId()
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/reservation/{reservationId}/delivery")
    @Override
    public ResponseEntity<UpdateDeliveryInfoResponse> updateDeliveryInfo(
            @PathVariable Long reservationId,
            @Valid @RequestBody UpdateDeliveryInfoRequest request,
            @RequestHeader("X-BOOKING-SESSION-ID") String bookingSessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UpdateDeliveryInfoResponse response = reservationService.updateDeliveryInfo(
                reservationId,
                request,
                userDetails.getUserId()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reservation/{reservationId}")
    @Override
    public ResponseEntity<CancelReservationResponse> cancelReservation(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CancelReservationResponse response = reservationService.cancelReservation(
                reservationId,
                userDetails.getUserId()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/schedule/{scheduleId}/seats/changes")
    @Override
    public ResponseEntity<SeatChangesResponse> getSeatChanges(
            @PathVariable Long scheduleId,
            @RequestParam(defaultValue = "0") Long sinceVersion,
            @RequestHeader("X-BOOKING-SESSION-ID") String bookingSessionId

            ) {
        SeatChangesResponse response = reservationService.getSeatChanges(
                scheduleId,
                sinceVersion
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-reservations")
    @Override
    public ResponseEntity<MyReservationListResponse> getMyReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Pageable pageable = PageRequest.of(page, size);

        MyReservationListResponse response = reservationService.getMyReservations(
                userDetails.getUserId(),
                pageable
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/reservation/{reservationId}")
    @Override
    public ResponseEntity<ReservationDetailResponse> getReservationDetail(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ReservationDetailResponse response = reservationService.getReservationDetail(
                reservationId,
                userDetails.getUserId()
        );

        return ResponseEntity.ok(response);
    }
}
