package back.kalender.domain.artist.entity;

import back.kalender.domain.user.entity.User;
import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "artist_follows")
public class ArtistFollow extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "artist_id", nullable = false)
    private Long artistId;
}

