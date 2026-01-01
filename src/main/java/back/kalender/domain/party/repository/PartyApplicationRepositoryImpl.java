package back.kalender.domain.party.repository;

import back.kalender.domain.party.entity.QParty;
import back.kalender.domain.party.enums.ApplicationStatus;
import back.kalender.domain.party.entity.PartyApplication;
import back.kalender.domain.party.entity.QPartyApplication;
import back.kalender.domain.party.enums.PartyStatus;
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
public class PartyApplicationRepositoryImpl implements PartyApplicationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<PartyApplication> findByApplicantIdAndStatusWithActiveParties(
            Long applicantId,
            ApplicationStatus status,
            Pageable pageable
    ) {
        QPartyApplication pa = QPartyApplication.partyApplication;
        QParty party = QParty.party;

        List<PartyApplication> content = queryFactory
                .selectFrom(pa)
                .join(party).on(pa.partyId.eq(party.id))
                .where(
                        pa.applicantId.eq(applicantId),
                        pa.status.eq(status),
                        party.status.in(
                                PartyStatus.RECRUITING,
                                PartyStatus.CLOSED
                        )
                )
                .orderBy(pa.updatedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(pa.count())
                .from(pa)
                .join(party).on(pa.partyId.eq(party.id))
                .where(
                        pa.applicantId.eq(applicantId),
                        pa.status.eq(status),
                        party.status.in(
                                PartyStatus.RECRUITING,
                                PartyStatus.CLOSED
                        )
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}