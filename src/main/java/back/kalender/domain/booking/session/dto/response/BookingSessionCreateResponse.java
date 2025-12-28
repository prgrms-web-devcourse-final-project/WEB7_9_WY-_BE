package back.kalender.domain.booking.session.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "BookingSession 생성 응답")
public record BookingSessionCreateResponse(
        @Schema(description = "예매 세션 ID", example = "bs_abc123xyz")
        String bookingSessionId
) {}
