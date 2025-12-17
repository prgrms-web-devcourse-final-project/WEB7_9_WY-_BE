package back.kalender.domain.booking.reservation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "배송/수령 정보 저장 요청")
public record UpdateDeliveryInfoRequest(
        @Schema(description = "수령 방법", example = "DELIVERY", allowableValues = {"DELIVERY", "PICKUP"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "수령 방법을 선택해주세요")
        String deliveryMethod,

        @Schema(description = "수령인 정보", requiredMode = Schema.RequiredMode.REQUIRED)
        @Valid
        @NotNull(message = "수령인 정보를 입력해주세요")
        RecipientInfo recipient
) {
    @Schema(description = "수령인 정보")
    public record RecipientInfo(
            @Schema(description = "수령인 이름", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "수령인 이름을 입력해주세요")
            String name,

            @Schema(description = "연락처", example = "010-1234-5678", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "연락처를 입력해주세요")
            @Pattern(regexp = "^01[0-9]-[0-9]{4}-[0-9]{4}$", message = "올바른 전화번호 형식이 아닙니다")
            String phone,

            @Schema(description = "주소 (배송 선택 시 필수)", example = "서울시 강남구 테헤란로 123")
            String address,

            @Schema(description = "우편번호 (배송 선택 시 필수)", example = "12345")
            String zipCode
    ) {
    }
}
