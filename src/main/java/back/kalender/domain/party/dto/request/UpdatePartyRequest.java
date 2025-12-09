package back.kalender.domain.party.dto.request;

import back.kalender.domain.party.entity.Gender;
import back.kalender.domain.party.entity.PreferredAge;
import back.kalender.domain.party.entity.TransportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "파티 수정 요청")
public record UpdatePartyRequest(

        @NotNull(message = "파티 ID는 필수입니다")
        @Schema(description = "파티 ID", example = "1", required = true)
        Long partyId,

        @Size(min = 2, max = 50, message = "파티 이름은 2자 이상 50자 이하여야 합니다")
        @Schema(description = "파티 이름", example = "지민이 최애")
        String partyName,

        @Size(max = 500, message = "설명은 500자 이하여야 합니다")
        @Schema(description = "파티 설명", example = "같이 즐겁게 콘서트 가요!")
        String description,

        @Size(max = 100, message = "출발 위치는 100자 이하여야 합니다")
        @Schema(description = "출발 위치", example = "강남역 3번출구")
        String departureLocation,

        @Size(max = 100, message = "도착 위치는 100자 이하여야 합니다")
        @Schema(description = "도착 위치", example = "잠실종합운동장")
        String arrivalLocation,

        @Schema(description = "이동 수단", implementation = TransportType.class)
        TransportType transportType,

        @Min(value = 2, message = "최대 인원은 2명 이상이어야 합니다")
        @Max(value = 10, message = "최대 인원은 10명 이하여야 합니다")
        @Schema(description = "최대 인원", example = "4")
        Integer maxMembers,

        @Schema(description = "선호 성별", implementation = Gender.class)
        Gender preferredGender,

        @Schema(description = "선호 연령대", implementation = PreferredAge.class)
        PreferredAge preferredAge
) {
}