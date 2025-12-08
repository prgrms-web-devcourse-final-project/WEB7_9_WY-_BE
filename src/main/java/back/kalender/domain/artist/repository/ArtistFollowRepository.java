package back.kalender.domain.artist.repository;

import back.kalender.domain.artist.entity.ArtistFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtistFollowRepository extends JpaRepository<ArtistFollow,Long> {

    //Optional<ArtistFollow> findByUserIdAndArtistId(Long userId, Long artistId);

    //List<ArtistFollow> findAllByUserId(Long userId);

}
