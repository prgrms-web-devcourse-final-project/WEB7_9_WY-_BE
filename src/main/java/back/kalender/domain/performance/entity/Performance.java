package back.kalender.domain.performance.entity;

import back.kalender.domain.artist.entity.Artist;
import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "performances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Performance extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_hall_id", nullable = false)
    private PerformanceHall performanceHall;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    private String title;

    @Column(name = "poster_image_url")
    private String posterImageUrl;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "running_time")
    private Integer runningTime;

    @Column(name = "booking_notice", columnDefinition = "TEXT")
    private String bookingNotice;

    @Column(name = "sales_start_time")
    private LocalDateTime salesStartTime;

    @Column(name = "sales_end_time")
    private LocalDateTime salesEndTime;
}
