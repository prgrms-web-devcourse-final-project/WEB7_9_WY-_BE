package back.kalender.domain.booking.session.interceptor;

import back.kalender.domain.booking.session.service.BookingSessionService;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class BookingSessionInterceptor implements HandlerInterceptor {

    public static final String HEADER_BOOKING_SESSION_ID = "X-BOOKING-SESSION-ID";

    private final BookingSessionService bookingSessionService;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        String bookingSessionId = request.getHeader(HEADER_BOOKING_SESSION_ID);

        if (bookingSessionId == null || bookingSessionId.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_BOOKING_SESSION);
        }

        // 예매창 체류권(존재/TTL)만 검증
        bookingSessionService.validateExists(bookingSessionId);

        return true;
    }
}