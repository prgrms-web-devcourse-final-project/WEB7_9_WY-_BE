package back.kalender.domain.party.repository;

import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.entity.PartyType;
import back.kalender.domain.party.entity.QParty;
import back.kalender.domain.party.entity.TransportType;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(party.scheduleId.eq(scheduleId));

        if (partyType != null) {
            builder.and(party.partyType.eq(partyType));
        }

        if (transportType != null) {
            builder.and(party.transportType.eq(transportType));
        }

        List<Party> content = queryFactory
                .selectFrom(party)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(party.createdAt.desc())
                .fetch();

        Long total = queryFactory
                .select(party.count())
                .from(party)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}