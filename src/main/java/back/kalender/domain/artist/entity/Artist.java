package back.kalender.domain.artist.entity;

import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "artists")
public class Artist extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    private String imageUrl;
}
