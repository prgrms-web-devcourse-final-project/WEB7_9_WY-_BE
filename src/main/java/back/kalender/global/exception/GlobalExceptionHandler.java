package back.kalender.global.exception;

import back.kalender.domain.booking.reservation.dto.response.HoldSeatsFailResponse;
import back.kalender.domain.booking.seatHold.exception.SeatHoldConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(ServiceException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.errorResponse(errorCode));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.errorResponse(ErrorCode.BAD_REQUEST));
    }

    @ExceptionHandler(SeatHoldConflictException.class)
    public ResponseEntity<HoldSeatsFailResponse> handleSeatHoldConflict(
            SeatHoldConflictException e
    ) {
        log.warn("[SeatHold] 좌석 충돌 발생 - reservationId={}, conflictCount={}",
                e.getReservationId(), e.getConflicts().size());

        // reservationId 정상 전달
        HoldSeatsFailResponse response = HoldSeatsFailResponse.of(
                e.getReservationId(),
                e.getConflicts()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }
}
