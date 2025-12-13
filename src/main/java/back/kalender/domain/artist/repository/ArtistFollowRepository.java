package back.kalender.domain.artist.repository;

import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.artist.entity.ArtistFollow;
import back.kalender.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistFollowRepository extends JpaRepository<ArtistFollow, Long> {

    // 팔로우한 아티스트 목록 조회
    List<ArtistFollow> findAllByUser(User user);

    // 팔로우 여부 확인
    boolean existsByUserAndArtist(User user, Artist artist);

    // 언팔로우
    void deleteByUserAndArtist(User user, Artist artist);
}
