package back.kalender.domain.party.enums;

import lombok.Getter;

@Getter
public enum ApplicationStatus {
    PENDING("대기중"),
    APPROVED("승인"),
    REJECTED("거절"),
    COMPLETED("종료"),
    KICKED("강퇴됨");

    private final String description;

    ApplicationStatus(String description) {
        this.description = description;
    }
}