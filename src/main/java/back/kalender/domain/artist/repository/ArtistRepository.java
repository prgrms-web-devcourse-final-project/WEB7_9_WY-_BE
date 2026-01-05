package back.kalender.domain.artist.repository;

import back.kalender.domain.artist.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist,Long> {
    boolean existsByName(String name);

    Optional<Artist> findByName(String name);
}
