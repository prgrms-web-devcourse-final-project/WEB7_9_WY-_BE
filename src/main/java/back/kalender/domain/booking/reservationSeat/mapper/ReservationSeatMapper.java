package back.kalender.domain.booking.reservationSeat.mapper;

import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;

public class ReservationSeatMapper {
    public static ReservationSeat create(Long reservationId, Long performanceSeatId, Integer price) {
        return ReservationSeat.builder()
                .reservationId(reservationId)
                .performanceSeatId(performanceSeatId)
                .price(price)
                .build();
    }
}
