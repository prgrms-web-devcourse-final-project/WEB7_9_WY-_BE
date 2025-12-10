package back.kalender.domain.party.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "내가 만든 파티 목록 조회 응답")
public record GetMyCreatedPartiesResponse(

        @Schema(description = "파티 목록")
        List<CreatedPartyItem> content,

        @Schema(description = "전체 요소 수", example = "1")
        Integer totalElements,

        @Schema(description = "전체 페이지 수", example = "1")
        Integer totalPages,

        @Schema(description = "현재 페이지 번호", example = "0")
        Integer pageNumber
) {

    @Schema(description = "생성한 파티 항목")
    public record CreatedPartyItem(

            @Schema(description = "파티 ID", example = "1")
            Long partyId,

            @Schema(description = "이벤트 정보")
            EventInfo event,

            @Schema(description = "파티 상세 정보")
            PartyDetailInfo partyInfo,

            @Schema(description = "신청 통계 정보")
            ApplicationStatistics statistics,

            @Schema(description = "파티 설명", example = "같이 즐겁게 콘서트 가요!")
            String description,

            @Schema(description = "채팅방 ID", example = "50")
            Long chatRoomId,

            @Schema(description = "파티 생성 일시", example = "2025-12-01T10:00:00")
            LocalDateTime createdAt
    ) {}

    @Schema(description = "이벤트 정보")
    public record EventInfo(

            @Schema(description = "행사 ID", example = "123")
            Long eventId,

            @Schema(description = "행사 제목", example = "BTS WORLD TOUR 2025")
            String eventTitle,

            @Schema(description = "장소", example = "잠실종합운동장")
            String venueName,

            @Schema(description = "행사 일시", example = "2025-12-15T19:00:00")
            LocalDateTime eventDateTime
    ) {}

    @Schema(description = "파티 상세 정보")
    public record PartyDetailInfo(

            @Schema(description = "파티 타입", example = "출발팟", allowableValues = {"출발팟", "복귀팟"})
            String partyType,

            @Schema(description = "출발 위치", example = "강남역 3번출구")
            String departureLocation,

            @Schema(description = "도착 위치", example = "잠실종합운동장")
            String arrivalLocation,

            @Schema(description = "이동 수단", example = "택시", allowableValues = {"택시", "카풀", "대중교통"})
            String transportType,

            @Schema(description = "최대 인원", example = "4")
            Integer maxMembers,

            @Schema(description = "현재 인원", example = "2")
            Integer currentMembers,

            @Schema(description = "파티 상태", example = "모집중", allowableValues = {"모집중", "모집마감", "종료"})
            String status
    ) {}

    @Schema(description = "신청 통계 정보")
    public record ApplicationStatistics(

            @Schema(description = "대기중인 신청 수", example = "3")
            Integer pendingApplicationsCount,

            @Schema(description = "승인된 멤버 수", example = "2")
            Integer acceptedMembersCount,

            @Schema(description = "거절된 신청 수", example = "1")
            Integer rejectedApplicationsCount
    ) {}
}