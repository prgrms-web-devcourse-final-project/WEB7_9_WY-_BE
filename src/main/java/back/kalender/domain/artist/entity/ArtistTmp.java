package back.kalender.domain.artist.entity;

import back.kalender.global.common.entity.BaseEntityTmp;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "artists")
public class ArtistTmp extends BaseEntityTmp {

    @Column(nullable = false, unique = true)
    private String name;

    private String imageUrl;
}
