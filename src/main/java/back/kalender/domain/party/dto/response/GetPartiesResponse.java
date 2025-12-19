package back.kalender.domain.party.dto.response;

import back.kalender.global.common.enums.Gender;
import back.kalender.domain.party.enums.PartyStatus;
import back.kalender.domain.party.enums.PartyType;
import back.kalender.domain.party.enums.TransportType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "파티 목록 조회 응답")
public record GetPartiesResponse(

        @Schema(description = "파티 목록")
        List<PartyItem> content,

        @Schema(description = "전체 요소 수", example = "45")
        Integer totalElements,

        @Schema(description = "전체 페이지 수", example = "3")
        Integer totalPages,

        @Schema(description = "현재 페이지 번호", example = "0")
        Integer pageNumber
) {

    @Schema(description = "파티 항목")
    public record PartyItem(

            @Schema(description = "파티 ID", example = "1")
            Long partyId,

            @Schema(description = "파티장 정보")
            Leader leader,

            @Schema(description = "이벤트 정보")
            Event event,

            @Schema(description = "파티 상세 정보")
            PartyInfo partyInfo,

            @Schema(description = "내 파티 여부", example = "false")
            Boolean isMyParty,

            @Schema(description = "신청 여부", example = "false")
            Boolean isApplied
    ) {}

    @Schema(description = "파티장 정보")
    public record Leader(

            @Schema(description = "사용자 ID", example = "100")
            Long userId,

            @Schema(description = "닉네임", example = "지민이 최애")
            String nickname,

            @Schema(description = "나이", example = "23")
            Integer age,

            @Schema(description = "성별", implementation = Gender.class)
            Gender gender,

            @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
            String profileImage
    ) {}

    @Schema(description = "이벤트 정보")
    public record Event(

            @Schema(description = "이벤트 ID", example = "123")
            Long eventId,

            @Schema(description = "이벤트 제목", example = "BTS WORLD TOUR 2025")
            String eventTitle,

            @Schema(description = "장소명", example = "잠실종합운동장")
            String venueName
    ) {}

    @Schema(description = "파티 상세 정보")
    public record PartyInfo(

            @Schema(description = "파티 타입", implementation = PartyType.class)
            PartyType partyType,

            @Schema(description = "파티 이름", example = "지민이 최애")
            String partyTitle,

            @Schema(description = "출발 위치", example = "강남역")
            String departureLocation,

            @Schema(description = "도착 위치", example = "잠실종합운동장")
            String arrivalLocation,

            @Schema(description = "이동 수단", implementation = TransportType.class)
            TransportType transportType,

            @Schema(description = "최대 인원", example = "4")
            Integer maxMembers,

            @Schema(description = "현재 인원", example = "2")
            Integer currentMembers,

            @Schema(description = "설명", example = "같이 즐겁게 콘서트 가요!")
            String description,

            @Schema(description = "파티 상태", implementation = PartyStatus.class)
            PartyStatus status
    ) {}
}