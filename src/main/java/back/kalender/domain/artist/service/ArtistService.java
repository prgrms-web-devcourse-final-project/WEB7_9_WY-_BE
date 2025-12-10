package back.kalender.domain.artist.service;

import back.kalender.domain.artist.dto.response.ArtistResponse;

import java.util.List;

public interface ArtistService {

    List<ArtistResponse> getAllArtists();

    List<ArtistResponse> getAllFollowedArtists(Long userId);

    void followArtist(Long userId, Long artistId);

    void unfollowArtist(Long userId, Long artistId);
}
