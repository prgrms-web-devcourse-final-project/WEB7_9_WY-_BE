package back.kalender.domain.artist.repository;

import back.kalender.domain.artist.entity.ArtistFollowTmp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistFollowRepositoryTmp extends JpaRepository<ArtistFollowTmp, Long> {
    List<ArtistFollowTmp> findAllByUserId(Long userId);

    boolean existsByUserIdAndArtistId(Long userId, Long artistId);

    void deleteByUserIdAndArtistId(Long userId, Long artistId);
}
