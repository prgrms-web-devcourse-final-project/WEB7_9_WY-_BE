package back.kalender.domain.booking.waitingRoom.dto;

public record QueueJoinResponse(
        String status,
        Long position,
        String qsid
) {}