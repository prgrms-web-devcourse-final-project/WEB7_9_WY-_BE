package back.kalender.domain.party.enums;

import lombok.Getter;

@Getter
public enum MemberRole {
    LEADER("파티장"),
    MEMBER("멤버");

    private final String description;

    MemberRole(String description) {
        this.description = description;
    }
}
