package back.kalender.domain.performance.performance.entity;

import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.performance.performanceHall.entity.PerformanceHall;
import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "performances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Performance extends BaseEntity {

    private Long performanceHallId;

    private Long artistId;

    private String title;

    private String posterImageUrl;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer runningTime;

    @Column( columnDefinition = "TEXT")
    private String bookingNotice;

    private LocalDateTime salesStartTime;

    private LocalDateTime salesEndTime;
}
