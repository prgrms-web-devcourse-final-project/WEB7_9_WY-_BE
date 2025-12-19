package back.kalender.domain.party.enums;

import lombok.Getter;

@Getter
public enum TransportType {
    TAXI("택시"),
    CARPOOL("카풀"),
    SUBWAY("지하철"),
    BUS("버스"),
    WALK("도보");

    private final String description;

    TransportType(String description) {
        this.description = description;
    }
}
