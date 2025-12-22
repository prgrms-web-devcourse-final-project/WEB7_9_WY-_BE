package back.kalender.domain.booking.web;

import back.kalender.domain.booking.reservation.entity.Reservation;
import back.kalender.domain.booking.reservation.repository.ReservationRepository;
import back.kalender.domain.booking.waitingRoom.service.QueueAccessService;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class BookingAccessInterceptor implements HandlerInterceptor {

    private static final String HEADER_QSID = "X-QSID";
    private final ReservationRepository reservationRepository;
    private final QueueAccessService queueAccessService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // QSID 헤더
        String qsid = request.getHeader(HEADER_QSID);
        if (qsid == null || qsid.isBlank()) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED);
        }

        // scheduleId 추출 - URL 경로에서
        Long scheduleId = extractScheduleId(request);

        if(scheduleId == null){
            Long reservationId = extractReservationId(request);
            if(reservationId != null){
                Reservation reservation = reservationRepository.findById(reservationId)
                        .orElseThrow(() -> new ServiceException(ErrorCode.RESERVATION_NOT_FOUND));
                scheduleId = reservation.getPerformanceScheduleId();
            }
        }

        // scheduleId 없으면 (마이페이지 등) 통과
        if (scheduleId == null) {
            return true;
        }

        // active ZSET 기반 접근 제어 (대기열 통과/활성 세션만 예매 가능)
        queueAccessService.checkSeatAccess(scheduleId, qsid);
        return true;
    }

    private Long extractScheduleId(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String key = "/schedule/";
        int index = uri.indexOf(key);
        if(index < 0){
            return null;
        }

        String remain = uri.substring(index + key.length());
        String[] parts = remain.split("/");
        if(parts.length == 0) return null;

        try {
            return Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long extractReservationId(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String key = "/reservation/";
        int index = uri.indexOf(key);
        if(index < 0){
            return null;
        }

        String remain = uri.substring(index + key.length());
        String[] parts = remain.split("/");
        if(parts.length == 0) return null;

        try {
            return Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
