package back.kalender.domain.booking.reservationSeat.entity;

import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="reservation_seats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationSeat extends BaseEntity {
    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "performance_seat_id", nullable = false)
    private Long performanceSeatId;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Builder
    public ReservationSeat(Long reservationId, Long performanceSeatId, Integer price) {
        this.reservationId = reservationId;
        this.performanceSeatId = performanceSeatId;
        this.price = price;
    }
    public static ReservationSeat create(Long reservationId, Long performanceSeatId, Integer price) {
        return ReservationSeat.builder()
                .reservationId(reservationId)
                .performanceSeatId(performanceSeatId)
                .price(price)
                .build();
    }
}
