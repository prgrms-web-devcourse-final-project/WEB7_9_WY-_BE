package back.kalender.domain.party.repository;

import back.kalender.domain.party.enums.ApplicationStatus;
import back.kalender.domain.party.entity.PartyApplication;
import back.kalender.domain.party.entity.QPartyApplication;
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
    public Page<PartyApplication> findActiveApplicationsByApplicantId(
            Long applicantId,
            Pageable pageable
    ) {
        QPartyApplication pa = QPartyApplication.partyApplication;

        // 데이터 조회
        List<PartyApplication> content = queryFactory
                .selectFrom(pa)
                .where(
                        pa.applicantId.eq(applicantId),
                        pa.status.notIn(
                                ApplicationStatus.COMPLETED,
                                ApplicationStatus.CANCELLED
                        )
                )
                .orderBy(pa.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // Count 쿼리
        JPAQuery<Long> countQuery = queryFactory
                .select(pa.count())
                .from(pa)
                .where(
                        pa.applicantId.eq(applicantId),
                        pa.status.notIn(
                                ApplicationStatus.COMPLETED,
                                ApplicationStatus.CANCELLED
                        )
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}