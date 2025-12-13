package back.kalender.domain.performance.schedule.entity;

import back.kalender.domain.performance.performane.entity.Performance;
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

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ScheduleStatus status;

}
