package back.kalender.domain.artist.repository;

import back.kalender.domain.artist.entity.ArtistFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistFollowRepository extends JpaRepository<ArtistFollow,Long> {

    List<ArtistFollow> findAllByUserId(Long userId);

    boolean existsByUserIdAndArtistId(Long userId, Long artistId);

    void deleteByUserIdAndArtistId(Long userId, Long artistId);
}
