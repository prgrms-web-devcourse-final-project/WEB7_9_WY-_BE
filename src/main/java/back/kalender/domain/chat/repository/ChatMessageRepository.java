package back.kalender.domain.chat.repository;

import back.kalender.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByPartyIdOrderByCreatedAtDesc(
            Long partyId,
            Pageable pageable
    );

    Optional<ChatMessage> findTopByPartyIdOrderByCreatedAtDesc(Long partyId);
}