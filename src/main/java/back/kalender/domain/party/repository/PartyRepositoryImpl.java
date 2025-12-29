package back.kalender.domain.party.repository;

import back.kalender.domain.party.dto.query.NotificationTarget;
import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.entity.QParty;
import back.kalender.domain.party.entity.QPartyMember;
import back.kalender.domain.party.enums.PartyStatus;
import back.kalender.domain.party.enums.PartyType;
import back.kalender.domain.party.enums.TransportType;
import back.kalender.domain.schedule.entity.QSchedule;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static back.kalender.domain.party.entity.QParty.party;

@Repository
@RequiredArgsConstructor
public class PartyRepositoryImpl implements PartyRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Party> findByScheduleIdWithFilters(
            Long scheduleId,
            PartyType partyType,
            TransportType transportType,
            PartyStatus status,  // 추가
            Pageable pageable
    ) {
        BooleanBuilder builder = new BooleanBuilder();

        // 스케줄 ID 필터
        builder.and(party.scheduleId.eq(scheduleId));

        // RECRUITING 상태 필터 추가
        if (status != null) {
            builder.and(party.status.eq(status));
        }

        // 파티 타입 필터
        if (partyType != null) {
            builder.and(party.partyType.eq(partyType));
        }

        // 이동 수단 필터
        if (transportType != null) {
            builder.and(party.transportType.eq(transportType));
        }

        // 최신순 정렬
        List<Party> content = queryFactory
                .selectFrom(party)
                .where(builder)
                .orderBy(party.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(party)
                .where(builder)
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<CompletedPartyWithType> findCompletedPartiesByUserId(
            Long userId,
            List<Long> joinedPartyIds,
            Pageable pageable
    ) {
        QParty party = QParty.party;

        List<CompletedPartyWithType> createdParties = queryFactory
                .select(Projections.constructor(
                        CompletedPartyWithType.class,
                        party,
                        Expressions.constant("CREATED")
                ))
                .from(party)
                .where(
                        party.leaderId.eq(userId),
                        party.status.eq(PartyStatus.COMPLETED)
                )
                .fetch();

        List<CompletedPartyWithType> joinedParties = new ArrayList<>();
        if (!joinedPartyIds.isEmpty()) {
            joinedParties = queryFactory
                    .select(Projections.constructor(
                            CompletedPartyWithType.class,
                            party,
                            Expressions.constant("JOINED")
                    ))
                    .from(party)
                    .where(
                            party.id.in(joinedPartyIds),
                            party.leaderId.ne(userId),
                            party.status.eq(PartyStatus.COMPLETED)
                    )
                    .fetch();
        }

        List<CompletedPartyWithType> allParties = new ArrayList<>();
        allParties.addAll(createdParties);
        allParties.addAll(joinedParties);

        allParties.sort(Comparator
                .comparing((CompletedPartyWithType p) -> p.party().getUpdatedAt())
                .reversed());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allParties.size());

        List<CompletedPartyWithType> pagedContent = start >= allParties.size()
                ? new ArrayList<>()
                : allParties.subList(start, end);

        return new PageImpl<>(pagedContent, pageable, allParties.size());
    }

    private BooleanExpression partyTypeEq(PartyType partyType) {
        return partyType != null ? party.partyType.eq(partyType) : null;
    }

    private BooleanExpression transportTypeEq(TransportType transportType) {
        return transportType != null ? party.transportType.eq(transportType) : null;
    }

    @Override
    public List<NotificationTarget> findNotificationTargets(LocalDateTime start, LocalDateTime end) {
        QSchedule schedule = QSchedule.schedule;
        QParty party = QParty.party;
        QPartyMember partyMember = QPartyMember.partyMember;

        return queryFactory
                .select(Projections.constructor(NotificationTarget.class,
                        partyMember.userId,
                        party.id,
                        schedule.title,
                        schedule.scheduleCategory,
                        schedule.scheduleTime
                ))
                .from(schedule)
                .join(party).on(party.scheduleId.eq(schedule.id))
                .join(partyMember).on(partyMember.partyId.eq(party.id))
                .where(
                        schedule.scheduleTime.between(start, end),
                        partyMember.leftAt.isNull(),
                        partyMember.kickedAt.isNull(),
                        party.status.notIn(PartyStatus.CANCELLED)
                )
                .fetch();
    }
}