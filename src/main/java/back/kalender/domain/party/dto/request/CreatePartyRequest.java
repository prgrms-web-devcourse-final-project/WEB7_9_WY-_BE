package back.kalender.domain.party.dto.request;

import back.kalender.domain.party.entity.Gender;
import back.kalender.domain.party.entity.PartyType;
import back.kalender.domain.party.entity.PreferredAge;
import back.kalender.domain.party.entity.TransportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "파티 생성 요청")
public record CreatePartyRequest(

        @NotNull(message = "일정 ID는 필수입니다")
        @Schema(description = "일정 ID", example = "123", required = true)
        Long scheduleId,

        @NotNull(message = "파티 타입은 필수입니다")
        @Schema(description = "파티 타입", required = true, implementation = PartyType.class)
        PartyType partyType,

        @NotBlank(message = "파티 이름은 필수입니다")
        @Size(min = 2, max = 50, message = "파티 이름은 2자 이상 50자 이하여야 합니다")
        @Schema(description = "파티 이름", example = "지민이 최애", required = true)
        String partyName,

        @Size(max = 500, message = "설명은 500자 이하여야 합니다")
        @Schema(description = "파티 설명", example = "같이 즐겁게 콘서트 가요!")
        String description,

        @NotBlank(message = "출발 위치는 필수입니다")
        @Size(max = 100, message = "출발 위치는 100자 이하여야 합니다")
        @Schema(description = "출발 위치", example = "강남역 3번출구", required = true)
        String departureLocation,

        @NotBlank(message = "도착 위치는 필수입니다")
        @Size(max = 100, message = "도착 위치는 100자 이하여야 합니다")
        @Schema(description = "도착 위치", example = "잠실종합운동장", required = true)
        String arrivalLocation,

        @NotNull(message = "이동 수단은 필수입니다")
        @Schema(description = "이동 수단", required = true, implementation = TransportType.class)
        TransportType transportType,

        @NotNull(message = "최대 인원은 필수입니다")
        @Min(value = 2, message = "최대 인원은 2명 이상이어야 합니다")
        @Max(value = 10, message = "최대 인원은 10명 이하여야 합니다")
        @Schema(description = "최대 인원", example = "4", required = true)
        Integer maxMembers,

        @NotNull(message = "선호 성별은 필수입니다")
        @Schema(description = "선호 성별", required = true, implementation = Gender.class)
        Gender preferredGender,

        @NotNull(message = "선호 연령대는 필수입니다")
        @Schema(description = "선호 연령대", required = true, implementation = PreferredAge.class)
        PreferredAge preferredAge
) {
}