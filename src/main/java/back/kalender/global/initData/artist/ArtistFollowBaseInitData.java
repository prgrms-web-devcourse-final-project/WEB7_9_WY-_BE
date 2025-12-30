package back.kalender.global.initData.artist;

import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.artist.entity.ArtistFollow;
import back.kalender.domain.artist.repository.ArtistFollowRepository;
import back.kalender.domain.artist.repository.ArtistRepository;
import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile({"prod", "dev"})
@Order(2)
@RequiredArgsConstructor
public class ArtistFollowBaseInitData {

    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final ArtistFollowRepository artistFollowRepository;

    @PostConstruct
    public void init() {
        if (artistFollowRepository.count() > 0) return;

        List<User> users = userRepository.findAll();
        List<Artist> artists = artistRepository.findAll();

        for (User user : users) {
            List<Artist> followed = artists.stream()
                .limit(3) // or 랜덤
                .toList();

            for (Artist artist : followed) {
                artistFollowRepository.save(
                    new ArtistFollow(user.getId(), artist.getId())
                );
            }
        }
    }
}