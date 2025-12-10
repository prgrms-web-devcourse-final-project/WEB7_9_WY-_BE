package back.kalender.domain.party.dto.response;

import back.kalender.domain.party.entity.ApplicationStatus;
import back.kalender.global.common.Enum.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "파티 신청자 목록 조회 응답")
public record GetApplicantsResponse(

        @Schema(description = "파티 ID", example = "1")
        Long partyId,

        @Schema(description = "신청자 목록")
        List<ApplicationInfo> applications,

        @Schema(description = "신청 요약 정보")
        ApplicationSummary summary
) {

    @Schema(description = "신청자 정보")
    public record ApplicationInfo(

            @Schema(description = "신청 ID", example = "101")
            Long applicationId,

            @Schema(description = "신청자 정보")
            ApplicantInfo applicant,

            @Schema(description = "신청 상태", implementation = ApplicationStatus.class)
            ApplicationStatus status
    ) {}

    @Schema(description = "신청자 상세 정보")
    public record ApplicantInfo(

            @Schema(description = "사용자 ID", example = "200")
            Long userId,

            @Schema(description = "닉네임", example = "명란 최애")
            String nickname,

            @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
            String profileImage,

            @Schema(description = "성별", implementation = Gender.class)
            Gender gender,

            @Schema(description = "나이", example = "23")
            Integer age
    ) {}

    @Schema(description = "신청 요약 정보")
    public record ApplicationSummary(

            @Schema(description = "전체 신청 수", example = "5")
            Integer totalApplications,

            @Schema(description = "대기중인 신청 수", example = "3")
            Integer pendingCount,

            @Schema(description = "승인된 신청 수", example = "2")
            Integer acceptedCount,

            @Schema(description = "거절된 신청 수", example = "0")
            Integer rejectedCount
    ) {}
}