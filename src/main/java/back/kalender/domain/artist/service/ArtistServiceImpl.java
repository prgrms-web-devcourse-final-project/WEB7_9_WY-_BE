package back.kalender.domain.artist.service;

import back.kalender.domain.artist.dto.response.ArtistResponse;
import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.artist.entity.ArtistFollow;
import back.kalender.domain.artist.repository.ArtistFollowRepository;
import back.kalender.domain.artist.repository.ArtistRepository;
import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.repository.UserRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final UserRepository userRepository;

    @Override
    public List<ArtistResponse> getAllArtists() {
        return artistRepository.findAll().stream()
                .map(ArtistResponse::from)
                .toList();
    }

    @Override
    public List<ArtistResponse> getAllFollowedArtists(Long userId) {
        User user = userRepository.getReferenceById(userId);

        return artistFollowRepository.findAllByUser(user).stream()
                .map(follow -> ArtistResponse.from(follow.getArtist()))
                .toList();
    }

    @Override
    @Transactional
    public void followArtist(Long userId, Long artistId) {

        User user = userRepository.getReferenceById(userId);

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ServiceException(ErrorCode.ARTIST_NOT_FOUND));

        if (artistFollowRepository.existsByUserAndArtist(user, artist)) {
            throw new ServiceException(ErrorCode.ALREADY_FOLLOWED);
        }

        ArtistFollow follow = new ArtistFollow(user, artist);
        artistFollowRepository.save(follow);
    }

    @Override
    @Transactional
    public void unfollowArtist(Long userId, Long artistId) {

        User user = userRepository.getReferenceById(userId);

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ServiceException(ErrorCode.ARTIST_NOT_FOUND));

        if (!artistFollowRepository.existsByUserAndArtist(user, artist)) {
            throw new ServiceException(ErrorCode.ARTIST_NOT_FOLLOWED);
        }

        // ✅ 실제 언팔로우
        artistFollowRepository.deleteByUserAndArtist(user, artist);
    }
}
