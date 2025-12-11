package back.kalender.global.common.Enum;

import lombok.Getter;

@Getter
public enum Gender {
    MALE("남성"),
    FEMALE("여성"),
    ANY("무관");

    private final String description;

    Gender(String description) {
        this.description = description;
    }
}
