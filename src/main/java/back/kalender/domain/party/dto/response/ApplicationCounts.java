package back.kalender.domain.party.dto.response;

public record ApplicationCounts(
        int pendingCount,
        int approvedCount,
        int rejectedCount
) {}
