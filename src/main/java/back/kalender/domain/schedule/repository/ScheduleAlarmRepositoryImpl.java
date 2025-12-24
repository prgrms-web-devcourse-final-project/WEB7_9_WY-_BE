package back.kalender.domain.schedule.repository;

import back.kalender.domain.party.dto.query.NotificationTarget;
import back.kalender.domain.schedule.entity.QSchedule;
import back.kalender.domain.schedule.entity.QScheduleAlarm;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ScheduleAlarmRepositoryImpl implements ScheduleAlarmRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<NotificationTarget> findScheduleNotificationTargets(LocalDateTime start, LocalDateTime end) {
        QSchedule schedule = QSchedule.schedule;
        QScheduleAlarm alarm = QScheduleAlarm.scheduleAlarm;

        return queryFactory
                .select(Projections.constructor(NotificationTarget.class,
                        alarm.userId,
                        schedule.title,
                        schedule.scheduleCategory,
                        schedule.scheduleTime
                ))
                .from(alarm)
                .join(schedule).on(alarm.scheduleId.eq(schedule.id))
                .where(schedule.scheduleTime.between(start, end))
                .fetch();
    }
}