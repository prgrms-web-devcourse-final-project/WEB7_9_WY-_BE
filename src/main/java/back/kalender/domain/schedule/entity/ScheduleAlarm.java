package back.kalender.domain.schedule.entity;

import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "schedule_alarms",
        uniqueConstraints = { @UniqueConstraint(columnNames = {"schedule_id", "user_id"}) })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleAlarm extends BaseEntity {

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public ScheduleAlarm(Long scheduleId, Long userId) {
        this.scheduleId = scheduleId;
        this.userId = userId;
    }
}