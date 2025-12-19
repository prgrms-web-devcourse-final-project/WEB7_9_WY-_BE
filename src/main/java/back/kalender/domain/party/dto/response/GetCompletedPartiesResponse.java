package back.kalender.domain.party.dto.response;

import back.kalender.domain.party.enums.PartyStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "종료된 파티 목록 응답")
public record GetCompletedPartiesResponse(
        @Schema(description = "종료된 파티 목록")
        List<CompletedPartyItem> parties,

        @Schema(description = "전체 개수")
        Integer totalElements,

        @Schema(description = "전체 페이지 수")
        Integer totalPages,

        @Schema(description = "현재 페이지 번호")
        Integer pageNumber
) {
    @Schema(description = "종료된 파티 정보")
    public record CompletedPartyItem(
            @Schema(description = "파티 ID", example = "1")
            Long partyId,

            @Schema(description = "파티 유형", example = "CREATED or JOINED")
            String participationType,

            @Schema(description = "이벤트 정보")
            EventInfo event,

            @Schema(description = "파티 상세 정보")
            PartyDetailInfo partyDetail,

            @Schema(description = "파티장 정보")
            LeaderInfo leader,

            @Schema(description = "파티 상태", example = "COMPLETED")
            PartyStatus status,

            @Schema(description = "종료 시간")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime completedAt,

            @Schema(description = "생성 시간")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime createdAt
    ) {}

    @Schema(description = "이벤트 정보")
    public record EventInfo(
            @Schema(description = "이벤트 ID", example = "1")
            Long eventId,

            @Schema(description = "이벤트 제목", example = "BTS 콘서트 2025")
            String title,

            @Schema(description = "장소", example = "잠실종합운동장")
            String location,

            @Schema(description = "일정 시간")
            LocalDateTime scheduleTime
    ) {}

    @Schema(description = "파티 상세 정보")
    public record PartyDetailInfo(
            @Schema(description = "파티 이름", example = "즐거운 BTS 콘서트")
            String partyName,

            @Schema(description = "파티 타입", example = "출발")
            String partyType,

            @Schema(description = "출발지", example = "강남역")
            String departureLocation,

            @Schema(description = "도착지", example = "잠실종합운동장")
            String arrivalLocation,

            @Schema(description = "최대 인원", example = "4")
            Integer maxMembers,

            @Schema(description = "현재 인원", example = "4")
            Integer currentMembers
    ) {}

    @Schema(description = "파티장 정보")
    public record LeaderInfo(
            @Schema(description = "파티장 ID", example = "1")
            Long leaderId,

            @Schema(description = "파티장 닉네임", example = "리더유저1")
            String nickname
    ) {}
}