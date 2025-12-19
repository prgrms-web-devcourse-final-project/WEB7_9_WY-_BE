package back.kalender.domain.party.dto.response;

import back.kalender.domain.party.enums.ApplicationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "신청한 파티 목록 조회 응답")
public record GetMyApplicationsResponse(

        @Schema(description = "신청 목록")
        List<ApplicationItem> content,

        @Schema(description = "전체 요소 수", example = "2")
        Integer totalElements,

        @Schema(description = "전체 페이지 수", example = "1")
        Integer totalPages,

        @Schema(description = "현재 페이지 번호", example = "0")
        Integer pageNumber
) {

    @Schema(description = "신청 항목")
    public record ApplicationItem(

            @Schema(description = "신청 ID", example = "101")
            Long applicationId,

            @Schema(description = "파티 정보")
            PartyInfo party,

            @Schema(description = "신청 상태", example = "대기중", allowableValues = {"대기중", "승인", "거절"})
            ApplicationStatus status,

            @Schema(description = "채팅방 ID (승인된 경우)", example = "60")
            Long chatRoomId
    ) {}

    @Schema(description = "파티 정보")
    public record PartyInfo(

            @Schema(description = "파티 ID", example = "10")
            Long partyId,

            @Schema(description = "파티장 정보")
            LeaderInfo leader,

            @Schema(description = "행사 정보")
            EventInfo event,

            @Schema(description = "파티 상세 정보")
            PartyDetailInfo partyInfo
    ) {}

    @Schema(description = "파티장 정보")
    public record LeaderInfo(

            @Schema(description = "사용자 ID", example = "100")
            Long userId,

            @Schema(description = "닉네임", example = "지민이 최애")
            String nickname,

            @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
            String profileImage
    ) {}

    @Schema(description = "행사 정보")
    public record EventInfo(

            @Schema(description = "행사 ID", example = "123")
            Long eventId,

            @Schema(description = "행사 제목", example = "BTS WORLD TOUR 2025")
            String eventTitle,

            @Schema(description = "장소", example = "잠실종합운동장")
            String venueName,

            @Schema(description = "행사 일시", example = "2025-12-15T19:00:00")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime eventDateTime
    ) {}

    @Schema(description = "파티 상세 정보")
    public record PartyDetailInfo(

            @Schema(description = "파티 타입", example = "출발팟", allowableValues = {"출발팟", "복귀팟"})
            String partyType,

            @Schema(description = "출발 위치", example = "강남역")
            String departureLocation,

            @Schema(description = "현재 인원", example = "2")
            Integer currentMembers,

            @Schema(description = "최대 인원", example = "4")
            Integer maxMembers
    ) {}
}