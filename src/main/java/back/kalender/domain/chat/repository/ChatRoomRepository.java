package back.kalender.domain.chat.repository;

import back.kalender.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByPartyId(Long partyId);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.partyId = :partyId AND cr.isActive = true")
    Optional<ChatRoom> findActiveByPartyId(@Param("partyId") Long partyId);

    boolean existsByPartyId(Long partyId);
}
