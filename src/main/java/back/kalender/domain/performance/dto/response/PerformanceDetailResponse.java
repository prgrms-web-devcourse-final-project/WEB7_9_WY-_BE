package back.kalender.domain.performance.dto.response;

import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.performance.entity.Performance;
import back.kalender.domain.performance.entity.PerformanceHall;
import back.kalender.domain.performance.entity.PerformanceSchedule;
import back.kalender.domain.performance.entity.PriceGrade;
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
        String bookingNotice,
        List<LocalDate> availableDates, // 예매 가능한 날짜 목록
        List<PerformanceScheduleResponse> schedules, // 모든 회차 정보
        Boolean isBookingOpen, // 예매 오픈 여부
        Long secondsUntilOpen // 예매 오픈까지 남은 시간 (초) - 24시간 이내일 때만 제공
        ) {

    @Schema(description = "아티스트 정보")
    public record ArtistInfo(
            Long artistId,
            String artistName
    ) {
        public static ArtistInfo from(Artist artist) {
            return new ArtistInfo(
                    artist.getId(),
                    artist.getName()
            );
        }
    }

    @Schema(description = "공연장 정보")
    public record PerformanceHallInfo(
            Long performanceHallId,
            String name,
            String address,
            String transportationInfo
    ) {
        public static PerformanceHallInfo from(PerformanceHall performanceHall) {
            return new PerformanceHallInfo(
                    performanceHall.getId(),
                    performanceHall.getName(),
                    performanceHall.getAddress(),
                    performanceHall.getTransportationInfo()
            );
        }
    }

    @Schema(description = "가격 등급 정보")
    public record PriceGradeInfo(
            Long priceGradeId,
            String gradeName,
            Integer price
    ) {
        public static PriceGradeInfo from(PriceGrade priceGrade) {
            return new PriceGradeInfo(
                    priceGrade.getId(),
                    priceGrade.getGradeName(),
                    priceGrade.getPrice()
            );
        }
    }

    public static PerformanceDetailResponse from(
            Performance performance,
            List<PriceGrade> priceGrades,
            List<LocalDate> availableDates,
            List<PerformanceSchedule> schedules
    ) {
        LocalDateTime now = LocalDateTime.now();
        boolean isBookingOpen = performance.getSalesStartTime() != null
                && now.isAfter(performance.getSalesStartTime())
                && (performance.getSalesEndTime() == null || now.isBefore(performance.getSalesEndTime()));

        Long secondsUntilOpen = null;
        if (!isBookingOpen && performance.getSalesStartTime() != null) {
            long seconds = java.time.Duration.between(now, performance.getSalesStartTime()).getSeconds();
            // 24시간(86400초) 이내일 때만 초단위 시간 제공
            if (seconds > 0 && seconds <= 86400) {
                secondsUntilOpen = seconds;
            }
            // 24시간 초과시에는 null (프론트에서 날짜로만 표시)
        }

        return new PerformanceDetailResponse(
                performance.getId(),
                performance.getTitle(),
                performance.getPosterImageUrl(),
                ArtistInfo.from(performance.getArtist()),
                performance.getStartDate(),
                performance.getEndDate(),
                performance.getRunningTime(),
                PerformanceHallInfo.from(performance.getPerformanceHall()),
                priceGrades.stream()
                        .map(PriceGradeInfo::from)
                        .toList(),
                performance.getSalesStartTime(),
                performance.getSalesEndTime(),
                performance.getBookingNotice(),
                availableDates,
                schedules.stream()
                        .map(PerformanceScheduleResponse::from)
                        .toList(),
                isBookingOpen,
                secondsUntilOpen
        );
    }
}
