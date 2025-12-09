package back.kalender.domain.performance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "공연 상세 정보 응답")
public record PerformanceDetailResponse(
        Long performanceId,
        String title,
        String posterImageUrl,
        ArtistInfo artist,
        LocalDate startDate,
        LocalDate endDate,
        Integer runningTime,
        PerformanceHallInfo performanceHall,
        List<PriceGradeInfo> priceGrades,
        LocalDateTime salesStartTime,
        LocalDateTime salesEndTime,
        String bookingNotice
        ) {

    @Schema(description = "아티스트 정보")
    public record ArtistInfo(
            Long artistId,
            String artistName
    ) {}

    @Schema(description = "공연장 정보")
    public record PerformanceHallInfo(
            Long performanceHallId,
            String name,
            String address,
            String transportationInfo
    ) {}

    @Schema(description = "가격 등급 정보")
    public record PriceGradeInfo(
            Long priceGradeId,
            String gradeName,
            Integer price
    ) {}
}
