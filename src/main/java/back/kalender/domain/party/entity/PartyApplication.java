package back.kalender.domain.party.entity;

import back.kalender.domain.party.enums.ApplicationStatus;
import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "party_applications",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_party_applicant", columnNames = {"party_id", "applicant_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyApplication extends BaseEntity {

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status;

    @Builder
    public PartyApplication(Long partyId, Long applicantId, Long leaderId) {
        this.partyId = partyId;
        this.applicantId = applicantId;
        this.leaderId = leaderId;
        this.status = ApplicationStatus.PENDING; // 기본값: 대기중
    }

    public static PartyApplication create(Long partyId, Long applicantId, Long leaderId) {
        return PartyApplication.builder()
                .partyId(partyId)
                .applicantId(applicantId)
                .leaderId(leaderId)
                .build();
    }

    public void kick() {
        if (this.status != ApplicationStatus.APPROVED) {
            throw new IllegalStateException("승인된 신청만 강퇴 처리할 수 있습니다.");
        }
        this.status = ApplicationStatus.KICKED;
    }

    public void approve() {
        if (this.status != ApplicationStatus.PENDING) {
            throw new IllegalStateException("대기 중인 신청만 승인할 수 있습니다.");
        }
        this.status = ApplicationStatus.APPROVED;
    }

    public void reject() {
        if (this.status != ApplicationStatus.PENDING) {
            throw new IllegalStateException("대기 중인 신청만 거절할 수 있습니다.");
        }
        this.status = ApplicationStatus.REJECTED;
    }

    public boolean isApproved() {
        return this.status == ApplicationStatus.APPROVED;
    }

    public boolean isProcessed() {
        return this.status != ApplicationStatus.PENDING;
    }
}