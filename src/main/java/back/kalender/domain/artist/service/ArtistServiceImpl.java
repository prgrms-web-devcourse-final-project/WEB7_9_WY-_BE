package back.kalender.domain.artist.service;

import back.kalender.domain.artist.dto.response.ArtistResponse;
import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.artist.entity.ArtistFollow;
import back.kalender.domain.artist.repository.ArtistFollowRepository;
import back.kalender.domain.artist.repository.ArtistRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;
    private final ArtistFollowRepository artistFollowRepository;

    @Override
    public List<ArtistResponse>  getAllArtists() {
        List<Artist>  artists = artistRepository.findAll();
        return artists.stream()
                .map(ArtistResponse::from)
                .toList();
    }

    @Override
    public List<ArtistResponse> getAllFollowedArtists(Long userId) {
        List<ArtistFollow> followedArtists = artistFollowRepository.findAllByUserId(userId);
        return followedArtists.stream()
                .map(followedArtist -> ArtistResponse.from(followedArtist.getArtist()))
                .toList();
    }

    @Override
    @Transactional
    public void followArtist(Long userId, Long artistId) {

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ServiceException(ErrorCode.ARTIST_NOT_FOUND));

        if (artistFollowRepository.existsByUserIdAndArtistId(userId, artistId)) {
            throw new ServiceException(ErrorCode.ALREADY_FOLLOWED);
        }

        ArtistFollow follow = new ArtistFollow(userId, artist);
        artistFollowRepository.save(follow);
    }

    @Override
    @Transactional
    public void unfollowArtist(Long userId, Long artistId) {

        boolean exists = artistFollowRepository.existsByUserIdAndArtistId(userId, artistId);

        if (!exists) {
            throw new ServiceException(ErrorCode.ARTIST_NOT_FOLLOWED);
        }

        artistFollowRepository.deleteByUserIdAndArtistId(userId,artistId);
    }
}
