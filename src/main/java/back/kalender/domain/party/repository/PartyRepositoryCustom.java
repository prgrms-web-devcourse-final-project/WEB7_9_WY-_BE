package back.kalender.domain.party.repository;

import back.kalender.domain.party.dto.query.NotificationTarget;
import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.enums.PartyType;
import back.kalender.domain.party.enums.TransportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PartyRepositoryCustom {

    Page<Party> findByScheduleIdWithFilters(
            Long scheduleId,
            PartyType partyType,
            TransportType transportType,
            Pageable pageable
    );

    Page<CompletedPartyWithType> findCompletedPartiesByUserId(
            Long userId,
            List<Long> joinedPartyIds,
            Pageable pageable
    );

    List<NotificationTarget> findNotificationTargets(LocalDateTime start, LocalDateTime end);

    record CompletedPartyWithType(Party party, String participationType) {}
}