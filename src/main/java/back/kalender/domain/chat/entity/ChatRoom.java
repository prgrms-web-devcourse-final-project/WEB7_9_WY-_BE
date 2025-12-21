package back.kalender.domain.chat.entity;

import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long partyId;

    @Column(nullable = false, length = 100)
    private String roomName;

    @Column(nullable = false)
    private Boolean isActive;


    public static ChatRoom create(Long partyId, String roomName) {
        return ChatRoom.builder()
                .partyId(partyId)
                .roomName(roomName)
                .isActive(true)
                .build();
    }

    @Builder
    private ChatRoom(Long partyId, String roomName, Boolean isActive) {
        this.partyId = partyId;
        this.roomName = roomName;
        this.isActive = isActive;
    }

    public void deactivate() {
        this.isActive = false;
    }

}
