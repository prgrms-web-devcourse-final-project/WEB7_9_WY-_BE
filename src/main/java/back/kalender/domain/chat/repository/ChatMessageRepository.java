package back.kalender.domain.chat.repository;

import back.kalender.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByPartyIdOrderByCreatedAtDesc(
            Long partyId,
            Pageable pageable
    );

    Optional<ChatMessage> findTopByPartyIdOrderByCreatedAtDesc(Long partyId);

    @Query("SELECT cm FROM ChatMessage cm " +
            "WHERE cm.id IN (" +
            "    SELECT MAX(cm2.id) FROM ChatMessage cm2 " +
            "    WHERE cm2.partyId IN :partyIds " +
            "    GROUP BY cm2.partyId" +
            ")")
    List<ChatMessage> findLastMessagesByPartyIds(@Param("partyIds") List<Long> partyIds);

}