package back.kalender.domain.artist.entity;

import jakarta.persistence.*;

public class ArtistFollow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //나중에 꼭 제거해야함
    @Column(name = "user_id", nullable = false)
    private Long userId;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id",nullable = false)
    private Artist artist;

    public ArtistFollow(Long userId, Artist artist) {
        this.userId = userId;
        this.artist = artist;
    }
}
