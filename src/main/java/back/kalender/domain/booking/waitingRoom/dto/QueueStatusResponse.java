package back.kalender.domain.booking.waitingRoom.dto;

public record QueueStatusResponse(
        String status,
        Long position,
        String waitingToken
) {}
