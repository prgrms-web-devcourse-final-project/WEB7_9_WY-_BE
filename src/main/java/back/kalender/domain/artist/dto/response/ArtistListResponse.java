package back.kalender.domain.artist.dto.response;

import java.util.List;

public record ArtistListResponse(
        List<ArtistResponse> artists
) {}
