package back.kalender.domain.artist.dto.response;

import back.kalender.domain.artist.entity.Artist;

public record ArtistResponse(
        Long artistId,
        String name,
        String imageUrl
) {
    public static ArtistResponse from(Artist artist) {
        return new ArtistResponse(artist.getId(), artist.getName(), artist.getImageUrl());
    }

}
