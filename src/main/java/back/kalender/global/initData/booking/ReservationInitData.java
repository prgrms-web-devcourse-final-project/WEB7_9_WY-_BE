package back.kalender.global.initData.booking;

import back.kalender.domain.booking.reservation.repository.ReservationRepository;
import back.kalender.domain.booking.reservationSeat.repository.ReservationSeatRepository;
import back.kalender.domain.booking.seatHold.repository.SeatHoldLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 애플리케이션 시작 시 예매 현황 초기화
 * - Reservation, ReservationSeat, SeatHoldLog 삭제
 * - Redis의 예매 관련 데이터 삭제 (booking session, queue, waiting 등)
 */
@Component
@Profile({"prod", "dev"})
@Order(10)  // 다른 초기화 데이터 이후에 실행
@RequiredArgsConstructor
@Slf4j
public class ReservationInitData implements ApplicationRunner {

    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final SeatHoldLogRepository seatHoldLogRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("예매 현황 초기화 시작");

        // 1. DB 데이터 삭제
        long reservationCount = reservationRepository.count();
        long reservationSeatCount = reservationSeatRepository.count();
        long seatHoldLogCount = seatHoldLogRepository.count();

        if (reservationCount > 0 || reservationSeatCount > 0 || seatHoldLogCount > 0) {
            log.info("예매 데이터 삭제 중 - Reservation: {}, ReservationSeat: {}, SeatHoldLog: {}",
                    reservationCount, reservationSeatCount, seatHoldLogCount);

            reservationSeatRepository.deleteAll();
            seatHoldLogRepository.deleteAll();
            reservationRepository.deleteAll();

            log.info("예매 데이터 삭제 완료");
        } else {
            log.info("삭제할 예매 데이터 없음");
        }

        // 2. Redis 예매 관련 데이터 삭제
        clearBookingRedisData();

        log.info("예매 현황 초기화 완료");
    }

    /**
     * Redis의 예매 관련 모든 데이터 삭제
     */
    private void clearBookingRedisData() {
        log.info("Redis 예매 데이터 삭제 시작");

        int deletedCount = 0;

        // booking:session:* 패턴 삭제
        Set<String> bookingSessionKeys = redisTemplate.keys("booking:session:*");
        if (bookingSessionKeys != null && !bookingSessionKeys.isEmpty()) {
            deletedCount += bookingSessionKeys.size();
            redisTemplate.delete(bookingSessionKeys);
            log.info("booking:session:* 키 {}개 삭제", bookingSessionKeys.size());
        }

        // booking:session:device:* 패턴 삭제
        Set<String> deviceKeys = redisTemplate.keys("booking:session:device:*");
        if (deviceKeys != null && !deviceKeys.isEmpty()) {
            deletedCount += deviceKeys.size();
            redisTemplate.delete(deviceKeys);
            log.info("booking:session:device:* 키 {}개 삭제", deviceKeys.size());
        }

        // booking:session:user:* 패턴 삭제
        Set<String> userKeys = redisTemplate.keys("booking:session:user:*");
        if (userKeys != null && !userKeys.isEmpty()) {
            deletedCount += userKeys.size();
            redisTemplate.delete(userKeys);
            log.info("booking:session:user:* 키 {}개 삭제", userKeys.size());
        }

        // active:* 패턴 삭제 (ZSet)
        Set<String> activeKeys = redisTemplate.keys("active:*");
        if (activeKeys != null && !activeKeys.isEmpty()) {
            deletedCount += activeKeys.size();
            redisTemplate.delete(activeKeys);
            log.info("active:* 키 {}개 삭제", activeKeys.size());
        }

        // queue:* 패턴 삭제 (ZSet)
        Set<String> queueKeys = redisTemplate.keys("queue:*");
        if (queueKeys != null && !queueKeys.isEmpty()) {
            deletedCount += queueKeys.size();
            redisTemplate.delete(queueKeys);
            log.info("queue:* 키 {}개 삭제", queueKeys.size());
        }

        // admitted:* 패턴 삭제 (Hash)
        Set<String> admittedKeys = redisTemplate.keys("admitted:*");
        if (admittedKeys != null && !admittedKeys.isEmpty()) {
            deletedCount += admittedKeys.size();
            redisTemplate.delete(admittedKeys);
            log.info("admitted:* 키 {}개 삭제", admittedKeys.size());
        }

        // waiting:* 패턴 삭제
        Set<String> waitingKeys = redisTemplate.keys("waiting:*");
        if (waitingKeys != null && !waitingKeys.isEmpty()) {
            deletedCount += waitingKeys.size();
            redisTemplate.delete(waitingKeys);
            log.info("waiting:* 키 {}개 삭제", waitingKeys.size());
        }

        // qsid:* 패턴 삭제
        Set<String> qsidKeys = redisTemplate.keys("qsid:*");
        if (qsidKeys != null && !qsidKeys.isEmpty()) {
            deletedCount += qsidKeys.size();
            redisTemplate.delete(qsidKeys);
            log.info("qsid:* 키 {}개 삭제", qsidKeys.size());
        }

        // device:* 패턴 삭제
        Set<String> deviceMappingKeys = redisTemplate.keys("device:*");
        if (deviceMappingKeys != null && !deviceMappingKeys.isEmpty()) {
            deletedCount += deviceMappingKeys.size();
            redisTemplate.delete(deviceMappingKeys);
            log.info("device:* 키 {}개 삭제", deviceMappingKeys.size());
        }

        log.info("Redis 예매 데이터 삭제 완료 - 총 {}개 키 삭제", deletedCount);
    }
}

