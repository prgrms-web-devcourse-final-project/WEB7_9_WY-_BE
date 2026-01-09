package back.kalender.domain.booking.performanceSeat.service;

import back.kalender.domain.booking.performanceSeat.dto.BlockSummaryResponse;
import back.kalender.domain.booking.performanceSeat.dto.PerformanceSeatResponse;
import back.kalender.domain.booking.performanceSeat.dto.SeatDetailResponse;
import back.kalender.domain.booking.performanceSeat.dto.SubBlockSummaryResponse;
import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.booking.waitingRoom.service.QueueAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceSeatQueryService {

    private final PerformanceSeatRepository performanceSeatRepository;
    private final QueueAccessService queueAccessService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String SEAT_SOLD_SET_KEY = "seat:sold:%d";
    private static final String SEAT_HOLD_OWNER_KEY_PATTERN = "seat:hold:owner:%d:*";

    private static final int IN_CLAUSE_CHUNK_SIZE = 1000;

    // =========================================================
    // (기존) 전체 좌석 조회 API - 기존 컨트롤러가 사용 (그대로 둬도 됨)
    // =========================================================
    @Transactional(readOnly = true)
    public List<PerformanceSeatResponse> getSeatsByScheduleId(Long scheduleId, String bookingSessionId) {
        queueAccessService.checkSeatAccess(scheduleId, bookingSessionId);

        Set<Long> soldSeatIds = loadSoldSeatIds(scheduleId);
        Set<Long> heldSeatIds = loadHeldSeatIds(scheduleId);

        return performanceSeatRepository.findAllByScheduleId(scheduleId)
                .stream()
                .map(seat -> {
                    SeatStatus finalStatus;

                    if (soldSeatIds.contains(seat.getId())) {
                        finalStatus = SeatStatus.SOLD;
                    } else if (heldSeatIds.contains(seat.getId())) {
                        finalStatus = SeatStatus.HOLD;
                    } else {
                        finalStatus = seat.getStatus();
                    }

                    return PerformanceSeatResponse.from(seat, finalStatus);
                })
                .toList();
    }

    // =========================================================
    // 1) 블록 요약 (DB 집계 + Redis SoT 반영)
    // =========================================================
    @Transactional(readOnly = true)
    public List<BlockSummaryResponse> getBlockSummaries(Long scheduleId, String bookingSessionId) {
        queueAccessService.checkSeatAccess(scheduleId, bookingSessionId);

        SeatStatus availableStatus = resolveAvailableStatus();

        // DB 집계
        List<PerformanceSeatRepository.BlockCountView> totals =
                performanceSeatRepository.countTotalByBlock(scheduleId);

        List<PerformanceSeatRepository.BlockCountView> dbAvailables =
                performanceSeatRepository.countDbAvailableByBlock(scheduleId, availableStatus);

        // Redis SoT (sold + hold) 좌석 id
        Set<Long> overrideIds = new HashSet<>();
        overrideIds.addAll(loadSoldSeatIds(scheduleId));
        overrideIds.addAll(loadHeldSeatIds(scheduleId));

        // overrideIds 중 "DB에서 AVAILABLE로 잡힌 좌석"이 있으면 DB available에서 차감해야 함
        Map<String, Long> overrideAvailableCountByBlock =
                countDbAvailableOverridesByBlockChunked(scheduleId, availableStatus, overrideIds);

        Map<String, Long> dbAvailableCountByBlock = dbAvailables.stream()
                .collect(Collectors.toMap(
                        v -> blockKey(v.getFloor(), v.getBlock()),
                        PerformanceSeatRepository.BlockCountView::getCnt
                ));

        return totals.stream()
                .map(v -> {
                    String key = blockKey(v.getFloor(), v.getBlock());
                    long total = v.getCnt();
                    long dbAvailable = dbAvailableCountByBlock.getOrDefault(key, 0L);
                    long overrideAvailable = overrideAvailableCountByBlock.getOrDefault(key, 0L);

                    long finalAvailable = Math.max(0L, dbAvailable - overrideAvailable);

                    return BlockSummaryResponse.of(v.getFloor(), v.getBlock(), total, finalAvailable);
                })
                .sorted(Comparator.comparing(BlockSummaryResponse::getFloor)
                        .thenComparing(BlockSummaryResponse::getBlock))
                .toList();
    }

    // =========================================================
    // 2) 서브블록 요약 (DB 집계 + Redis SoT 반영)
    // =========================================================
    @Transactional(readOnly = true)
    public List<SubBlockSummaryResponse> getSubBlockSummaries(Long scheduleId, String block, String bookingSessionId) {
        queueAccessService.checkSeatAccess(scheduleId, bookingSessionId);

        SeatStatus availableStatus = resolveAvailableStatus();

        // DB 집계
        List<PerformanceSeatRepository.SubBlockCountView> totals =
                performanceSeatRepository.countTotalBySubBlock(scheduleId, block);

        List<PerformanceSeatRepository.SubBlockCountView> dbAvailables =
                performanceSeatRepository.countDbAvailableBySubBlock(scheduleId, block, availableStatus);

        // Redis SoT seat ids
        Set<Long> overrideIds = new HashSet<>();
        overrideIds.addAll(loadSoldSeatIds(scheduleId));
        overrideIds.addAll(loadHeldSeatIds(scheduleId));

        Map<String, Long> overrideAvailableCountBySubBlock =
                countDbAvailableOverridesBySubBlockChunked(scheduleId, block, availableStatus, overrideIds);

        Map<String, Long> dbAvailableCountBySubBlock = dbAvailables.stream()
                .collect(Collectors.toMap(
                        PerformanceSeatRepository.SubBlockCountView::getSubBlock,
                        PerformanceSeatRepository.SubBlockCountView::getCnt
                ));

        return totals.stream()
                .map(v -> {
                    String subBlock = v.getSubBlock();
                    long total = v.getCnt();
                    long dbAvailable = dbAvailableCountBySubBlock.getOrDefault(subBlock, 0L);
                    long overrideAvailable = overrideAvailableCountBySubBlock.getOrDefault(subBlock, 0L);

                    long finalAvailable = Math.max(0L, dbAvailable - overrideAvailable);

                    return SubBlockSummaryResponse.of(subBlock, total, finalAvailable);
                })
                .sorted(Comparator.comparing(SubBlockSummaryResponse::getSubBlock))
                .toList();
    }

    // =========================================================
    // 3) 좌석 상세 (필요한 subBlock만 DB에서 조회)
    // =========================================================
    @Transactional(readOnly = true)
    public List<SeatDetailResponse> getSeatDetails(Long scheduleId, String block, String subBlock, String bookingSessionId) {
        queueAccessService.checkSeatAccess(scheduleId, bookingSessionId);

        return performanceSeatRepository.findSeatDetails(scheduleId, block, subBlock)
                .stream()
                .map(v -> SeatDetailResponse.of(
                        v.getSeatId(),
                        v.getRowNumber(),
                        v.getSeatNumber(),
                        v.getPriceGradeId()
                ))
                .toList();
    }

    // ---------------------------
    // Redis helpers
    // ---------------------------
    private Set<Long> loadSoldSeatIds(Long scheduleId) {
        String soldSetKey = String.format(SEAT_SOLD_SET_KEY, scheduleId);
        Set<String> soldSeatIdsStr = redisTemplate.opsForSet().members(soldSetKey);

        if (soldSeatIdsStr == null || soldSeatIdsStr.isEmpty()) return Set.of();

        Set<Long> result = soldSeatIdsStr.stream()
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        log.debug("[SeatQuery] Redis SOLD 좌석 수 - scheduleId={}, count={}", scheduleId, result.size());
        return result;
    }

    private Set<Long> loadHeldSeatIds(Long scheduleId) {
        String holdPattern = String.format(SEAT_HOLD_OWNER_KEY_PATTERN, scheduleId);
        Set<String> holdKeys = redisTemplate.keys(holdPattern);

        if (holdKeys == null || holdKeys.isEmpty()) return Set.of();

        Set<Long> result = holdKeys.stream()
                .map(key -> {
                    String[] parts = key.split(":");
                    return Long.parseLong(parts[4]); // seat:hold:owner:{scheduleId}:{seatId}
                })
                .collect(Collectors.toSet());

        log.debug("[SeatQuery] Redis HOLD 좌석 수 - scheduleId={}, count={}", scheduleId, result.size());
        return result;
    }

    // ---------------------------
    // override 집계 (IN 절 chunk)
    // ---------------------------
    private Map<String, Long> countDbAvailableOverridesByBlockChunked(
            Long scheduleId,
            SeatStatus availableStatus,
            Set<Long> overrideIds
    ) {
        if (overrideIds == null || overrideIds.isEmpty()) return Map.of();

        List<Long> ids = new ArrayList<>(overrideIds);
        Map<String, Long> acc = new HashMap<>();

        for (int i = 0; i < ids.size(); i += IN_CLAUSE_CHUNK_SIZE) {
            List<Long> chunk = ids.subList(i, Math.min(i + IN_CLAUSE_CHUNK_SIZE, ids.size()));
            List<PerformanceSeatRepository.BlockCountView> rows =
                    performanceSeatRepository.countDbAvailableOverridesByBlock(scheduleId, availableStatus, chunk);

            for (var r : rows) {
                String key = blockKey(r.getFloor(), r.getBlock());
                acc.merge(key, r.getCnt(), Long::sum);
            }
        }
        return acc;
    }

    private Map<String, Long> countDbAvailableOverridesBySubBlockChunked(
            Long scheduleId,
            String block,
            SeatStatus availableStatus,
            Set<Long> overrideIds
    ) {
        if (overrideIds == null || overrideIds.isEmpty()) return Map.of();

        List<Long> ids = new ArrayList<>(overrideIds);
        Map<String, Long> acc = new HashMap<>();

        for (int i = 0; i < ids.size(); i += IN_CLAUSE_CHUNK_SIZE) {
            List<Long> chunk = ids.subList(i, Math.min(i + IN_CLAUSE_CHUNK_SIZE, ids.size()));
            List<PerformanceSeatRepository.SubBlockCountView> rows =
                    performanceSeatRepository.countDbAvailableOverridesBySubBlock(scheduleId, block, availableStatus, chunk);

            for (var r : rows) {
                acc.merge(r.getSubBlock(), r.getCnt(), Long::sum);
            }
        }
        return acc;
    }

    private String blockKey(int floor, String block) {
        return floor + "|" + block;
    }

    /**
     * 프로젝트 SeatStatus 값에 맞춰 자동 선택.
     * - AVAILABLE 있으면 AVAILABLE
     * - 없으면 OPEN 시도
     */
    private SeatStatus resolveAvailableStatus() {
        try {
            return SeatStatus.valueOf("AVAILABLE");
        } catch (IllegalArgumentException ignored) {
            try {
                return SeatStatus.valueOf("OPEN");
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("SeatStatus에 AVAILABLE/OPEN 중 하나가 필요합니다.");
            }
        }
    }
}
