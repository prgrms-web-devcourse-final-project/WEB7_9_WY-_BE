package back.kalender.domain.party.dto.response;

import back.kalender.domain.party.entity.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

public record ApplyToPartyResponse(
        @Schema(description = "신청자 닉네임", example = "이치로")
        String applicantName,

        @Schema(description = "신청자 나이", example = "23")
        int applicantAge,

        @Schema(description = "신청자 성별", example = "남성")
        Gender gender,

        @Schema(description = "파티 이름", example = "BTS 공연")
        String partyTitle
) {
}
