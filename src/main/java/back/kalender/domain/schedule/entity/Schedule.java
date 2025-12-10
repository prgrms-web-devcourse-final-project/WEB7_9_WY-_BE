package back.kalender.domain.schedule.entity;

import back.kalender.global.common.entity.BaseEntityTmp;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedules")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

    private LocalDate date;
    //TODO: scheduleTime, date 차이 명확히 알아두기, 실제 일정 날짜는 3일이고 티켓팅은 1일일 때 데이터 처리 어떻게 할지 고민하기
}
