package back.kalender.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedule")
@Getter
@NoArgsConstructor
public class ScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scheduleId;
    private Long artistId;
    private Long performanceId;
    private String title;
    private String category;
    private String link;

    @Column(name = "schedule_time")
    private LocalDateTime scheduleTime;
}
