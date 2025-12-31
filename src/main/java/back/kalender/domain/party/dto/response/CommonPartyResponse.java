package back.kalender.domain.party.dto.response;

import back.kalender.domain.party.enums.ApplicationStatus;
import back.kalender.domain.party.enums.PartyStatus;
import back.kalender.domain.party.enums.PartyType;
import back.kalender.domain.party.enums.PreferredAge;
import back.kalender.domain.party.enums.TransportType;
import back.kalender.global.common.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "파티 목록 공통 응답")
public record CommonPartyResponse(

        @Schema(description = "파티 목록")
        List<PartyItem> parties,

        @Schema(description = "전체 개수", example = "50")
        Integer totalElements,

        @Schema(description = "전체 페이지 수", example = "3")
        Integer totalPages,

        @Schema(description = "현재 페이지", example = "0")
        Integer pageNumber
) {

    @Schema(description = "파티 항목")
    public record PartyItem(

            @Schema(description = "파티 ID", example = "1")
            Long partyId,

            @Schema(description = "일정 정보")
            ScheduleInfo schedule,

            @Schema(description = "리더 정보")
            LeaderInfo leader,

            @Schema(description = "파티 상세 정보")
            PartyDetail partyDetail,

            @Schema(description = "내가 만든 파티인지 여부", example = "false")
            Boolean isMyParty,

            @Schema(description = "신청한 파티인지 여부", example = "true")
            Boolean isApplied,

            @Schema(description = "참여 타입 (CREATED: 내가 만든 파티, JOINED: 참여한 파티, PENDING: 신청중)", example = "JOINED")
            String participationType,

            @Schema(description = "신청 ID (신청한 파티인 경우에만 존재)", example = "123", nullable = true)
            Long applicationId,

            @Schema(description = "신청 상태 (신청한 파티인 경우에만 존재)", example = "PENDING", nullable = true)
            ApplicationStatus applicationStatus
    ) {}

    @Schema(description = "일정 정보")
    public record ScheduleInfo(

            @Schema(description = "일정 ID", example = "1")
            Long scheduleId,

            @Schema(description = "일정 제목", example = "BTS 콘서트 2025")
            String title
    ) {}

    @Schema(description = "리더 정보")
    public record LeaderInfo(

            @Schema(description = "리더 ID", example = "1")
            Long leaderId,

            @Schema(description = "리더 닉네임", example = "지민이최애")
            String nickname
    ) {}

    @Schema(description = "파티 상세 정보")
    public record PartyDetail(

            @Schema(description = "파티 타입", example = "LEAVE")
            PartyType partyType,

            @Schema(description = "파티 이름", example = "즐거운 BTS 콘서트")
            String partyName,

            @Schema(description = "출발 장소", example = "강남역")
            String departureLocation,

            @Schema(description = "도착 장소", example = "잠실종합운동장")
            String arrivalLocation,

            @Schema(description = "교통수단", example = "SUBWAY")
            TransportType transportType,

            @Schema(description = "최대 인원", example = "4")
            Integer maxMembers,

            @Schema(description = "현재 인원", example = "2")
            Integer currentMembers,

            @Schema(description = "선호 성별", example = "ANY")
            Gender preferredGender,

            @Schema(description = "선호 연령대", example = "TWENTY")
            PreferredAge preferredAge,

            @Schema(description = "파티 상태", example = "RECRUITING")
            PartyStatus status,

            @Schema(description = "파티 설명", example = "같이 즐겁게 콘서트 보러 가요!")
            String description
    ) {}
}