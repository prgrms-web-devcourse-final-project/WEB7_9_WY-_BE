package back.kalender.domain.artist.service;

import back.kalender.domain.artist.dto.response.ArtistResponse;
import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.artist.entity.ArtistFollow;
import back.kalender.domain.artist.repository.ArtistFollowRepository;
import back.kalender.domain.artist.repository.ArtistRepository;
import back.kalender.domain.user.repository.UserRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ArtistServiceTest {

    @InjectMocks
    private ArtistService artistService;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ArtistFollowRepository artistFollowRepository;

    @Mock
    private UserRepository userRepository;

    /* ======================
       전체 아티스트 조회
       ====================== */

    @DisplayName("전체 아티스트 조회 성공")
    @Test
    void getAllArtists_success() {
        Artist artist1 = new Artist("아이유", "img1");
        Artist artist2 = new Artist("태연", "img2");

        given(artistRepository.findAll())
                .willReturn(List.of(artist1, artist2));

        List<ArtistResponse> result = artistService.getAllArtists();

        assertThat(result).hasSize(2);
    }

    /* ======================
       팔로우한 아티스트 조회
       ====================== */

    @DisplayName("팔로우한 아티스트 조회 성공")
    @Test
    void getAllFollowedArtists_success() {
        Long userId = 1L;
        Long artistId = 10L;

        ArtistFollow follow = new ArtistFollow(userId, artistId);
        Artist artist = new Artist("아이유", "img");

        given(artistFollowRepository.findAllByUserId(userId))
                .willReturn(List.of(follow));
        given(artistRepository.findAllById(List.of(artistId)))
                .willReturn(List.of(artist));

        List<ArtistResponse> result =
                artistService.getAllFollowedArtists(userId);

        assertThat(result).hasSize(1);
    }

    /* ======================
       아티스트 팔로우
       ====================== */

    @DisplayName("아티스트 팔로우 성공")
    @Test
    void followArtist_success() {
        Long userId = 1L;
        Long artistId = 10L;

        given(artistRepository.existsById(artistId))
                .willReturn(true);
        given(artistFollowRepository.existsByUserIdAndArtistId(userId, artistId))
                .willReturn(false);

        artistService.followArtist(userId, artistId);

        then(artistFollowRepository).should()
                .save(any(ArtistFollow.class));
    }

    @DisplayName("아티스트 팔로우 실패 - 이미 팔로우한 경우")
    @Test
    void followArtist_alreadyFollowed() {
        Long userId = 1L;
        Long artistId = 10L;

        given(artistRepository.existsById(artistId))
                .willReturn(true);
        given(artistFollowRepository.existsByUserIdAndArtistId(userId, artistId))
                .willReturn(true);

        ServiceException exception = catchThrowableOfType(
                () -> artistService.followArtist(userId, artistId),
                ServiceException.class
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_FOLLOWED);
    }

    /* ======================
       아티스트 언팔로우
       ====================== */

    @DisplayName("아티스트 언팔로우 성공")
    @Test
    void unfollowArtist_success() {
        Long userId = 1L;
        Long artistId = 10L;

        given(artistFollowRepository.existsByUserIdAndArtistId(userId, artistId))
                .willReturn(true);

        artistService.unfollowArtist(userId, artistId);

        then(artistFollowRepository).should()
                .deleteByUserIdAndArtistId(userId, artistId);
    }

    @DisplayName("아티스트 언팔로우 실패 - 팔로우 상태 아님")
    @Test
    void unfollowArtist_notFollowed() {
        Long userId = 1L;
        Long artistId = 10L;

        given(artistFollowRepository.existsByUserIdAndArtistId(userId, artistId))
                .willReturn(false);

        ServiceException exception = catchThrowableOfType(
                () -> artistService.unfollowArtist(userId, artistId),
                ServiceException.class
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.ARTIST_NOT_FOLLOWED);
    }
}
