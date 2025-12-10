package back.kalender.domain.schedule.entity;

import back.kalender.global.common.entity.BaseEntityTmp;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "schedules",
        indexes = @Index(name = "idx_schedule_artist_time", columnList = "artistId, scheduleTime")
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Schedule extends BaseEntityTmp {

    @Column(nullable = false)
    private Long artistId;

    private Long performanceId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleCategory scheduleCategory;

    private String link;

    @Column(name = "schedule_time")
    private LocalDateTime scheduleTime;

    private String location;
}
