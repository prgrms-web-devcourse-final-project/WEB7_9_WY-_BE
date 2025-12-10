package back.kalender.domain.party.entity;

import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Column(name = "is_kicked", nullable = false)
    private Boolean isKicked;

    @Column(name = "kicked_at")
    private LocalDateTime kickedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public PartyMember(Long partyId, Long userId, MemberRole role) {
        this.partyId = partyId;
        this.userId = userId;
        this.role = role;
        this.isKicked = false;
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

    public void leave() {
        this.leftAt = LocalDateTime.now();
    }

    public void kick() {
        this.isKicked = true;
        this.kickedAt = LocalDateTime.now();
        this.leftAt = LocalDateTime.now();
    }

    public boolean isLeader() {
        return this.role == MemberRole.LEADER;
    }

    public boolean hasLeft() {
        return this.leftAt != null;
    }

    public boolean wasKicked() {
        return this.isKicked != null && this.isKicked;
    }
}