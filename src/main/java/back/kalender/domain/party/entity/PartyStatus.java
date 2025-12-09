package back.kalender.domain.party.entity;

import lombok.Getter;

@Getter
public enum PartyStatus {
    RECRUITING("모집중"),
    CLOSED("모집마감"),
    COMPLETED("종료"),
    CANCELLED("취소됨");

    private final String description;

    PartyStatus(String description) {
        this.description = description;
    }
}