package back.kalender.domain.party.entity;

import lombok.Getter;

@Getter
public enum ApplicationStatus {
    PENDING("대기중"),
    APPROVED("승인"),
    REJECTED("거절"),
    CANCELLED("취소됨");

    private final String description;

    ApplicationStatus(String description) {
        this.description = description;
    }
}