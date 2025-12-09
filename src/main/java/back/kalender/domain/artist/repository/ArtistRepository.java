package back.kalender.domain.artist.repository;

import back.kalender.domain.artist.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist,Long> {
}
