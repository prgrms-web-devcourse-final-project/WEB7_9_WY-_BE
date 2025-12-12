package back.kalender.domain.party.repository;

import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.entity.PartyType;
import back.kalender.domain.party.entity.QParty;
import back.kalender.domain.party.entity.TransportType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PartyRepositoryImpl implements PartyRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Party> findByScheduleIdWithFilters(
            Long scheduleId,
            PartyType partyType,
            TransportType transportType,
            Pageable pageable
    ) {
        QParty party = QParty.party;

        List<Party> content = queryFactory
                .selectFrom(party)
                .where(
                        scheduleIdEq(scheduleId),
                        partyTypeEq(partyType),
                        transportTypeEq(transportType)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(party.createdAt.desc())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(party.count())
                .from(party)
                .where(
                        scheduleIdEq(scheduleId),
                        partyTypeEq(partyType),
                        transportTypeEq(transportType)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression scheduleIdEq(Long scheduleId) {
        return scheduleId != null ? QParty.party.scheduleId.eq(scheduleId) : null;
    }

    private BooleanExpression partyTypeEq(PartyType partyType) {
        return partyType != null ? QParty.party.partyType.eq(partyType) : null;
    }

    private BooleanExpression transportTypeEq(TransportType transportType) {
        return transportType != null ? QParty.party.transportType.eq(transportType) : null;
    }
}