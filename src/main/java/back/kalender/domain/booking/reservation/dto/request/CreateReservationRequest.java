package back.kalender.domain.booking.reservation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "예매 생성 요청")
public record CreateReservationRequest(
) {
}
