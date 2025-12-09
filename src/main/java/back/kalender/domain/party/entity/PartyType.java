package back.kalender.domain.party.entity;

public enum PartyType {
    LEAVE("출발팟"),
    ARRIVE("복귀팟");

    private final String description;

    PartyType(String description) {
        this.description = description;
    }
}
