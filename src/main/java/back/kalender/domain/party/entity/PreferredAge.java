package back.kalender.domain.party.entity;

public enum PreferredAge {
    TEEN("10대"),
    TWENTY("20대"),
    THIRTY("30대"),
    FORTY("40대"),
    FIFTY_PLUS("50대 이상"),
    NONE("무관");

    private final String description;

    PreferredAge(String description) {
        this.description = description;
    }
}
