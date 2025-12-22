package back.kalender.domain.booking.seatHold.repository;

import back.kalender.domain.booking.seatHold.entity.SeatHoldLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatHoldLogRepository extends JpaRepository<SeatHoldLog, Long> {

    // 좌석별 홀드 로그 조회
    List<SeatHoldLog> findByPerformanceSeatId(Long performanceSeatId);

    // 사용자별 홀드 로그 조회
    List<SeatHoldLog> findByUserId(Long userId);
}
