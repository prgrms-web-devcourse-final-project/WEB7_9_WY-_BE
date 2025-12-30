package back.kalender.domain.party.entity;

import back.kalender.domain.party.enums.MemberRole;
import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "party_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_party_user", columnNames = {"party_id", "user_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyMember extends BaseEntity {

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MemberRole role;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "kicked_at")
    private LocalDateTime kickedAt;

    @Builder
    public PartyMember(Long partyId, Long userId, MemberRole role) {
        this.partyId = partyId;
        this.userId = userId;
        this.role = role;
    }

    public static PartyMember createLeader(Long partyId, Long leaderId) {
        return PartyMember.builder()
                .partyId(partyId)
                .userId(leaderId)
                .role(MemberRole.LEADER)
                .build();
    }

    public static PartyMember createMember(Long partyId, Long userId) {
        return PartyMember.builder()
                .partyId(partyId)
                .userId(userId)
                .role(MemberRole.MEMBER)
                .build();
    }

    public void leave(LocalDateTime leftAt) {
        this.leftAt = leftAt;
    }

    public void kick(LocalDateTime kickedAt) {
        this.kickedAt = kickedAt;
    }
}