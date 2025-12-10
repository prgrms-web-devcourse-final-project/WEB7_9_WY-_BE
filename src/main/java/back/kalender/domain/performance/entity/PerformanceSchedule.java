package back.kalender.domain.performance.entity;

import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "performance_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PerformanceSchedule extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    @Column(name = "performance_date", nullable = false)
    private LocalDate performanceDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name="performance_no")
    private Integer performanceNo;

    private Integer availableSeats;

    private Integer totalSeats;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ScheduleStatus status;

    // 예매 가능 여부 체크 메서드
    public boolean isBookingAvailable() {
        return this.status == ScheduleStatus.AVAILABLE && this.availableSeats > 0;
    }

}
