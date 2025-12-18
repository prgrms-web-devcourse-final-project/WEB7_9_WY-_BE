package back.kalender.domain.booking.seatHold.service;

import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.booking.reservation.dto.request.HoldSeatsRequest;
import back.kalender.domain.booking.reservation.dto.response.HoldSeatsFailResponse;
import back.kalender.domain.booking.reservation.dto.response.HoldSeatsResponse;
import back.kalender.domain.booking.reservation.entity.Reservation;
import back.kalender.domain.booking.reservation.entity.ReservationStatus;
import back.kalender.domain.booking.reservation.mapper.ReservationMapper;
import back.kalender.domain.booking.reservation.repository.ReservationRepository;
import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;
import back.kalender.domain.booking.reservationSeat.repository.ReservationSeatRepository;
import back.kalender.domain.booking.seatHold.entity.SeatHoldLog;
import back.kalender.domain.booking.seatHold.exception.SeatHoldConflictException;
import back.kalender.domain.booking.seatHold.exception.SeatNotAvailableException;
import back.kalender.domain.booking.seatHold.mapper.SeatHoldMapper;
import back.kalender.domain.booking.seatHold.repository.SeatHoldLogRepository;
import back.kalender.domain.performance.priceGrade.entity.PriceGrade;
import back.kalender.domain.performance.priceGrade.repository.PriceGradeRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * V1: 분산 락 + 폴링 버전
 *
 * <Redis 키 설계>
 * - seat:lock:{scheduleId}:{seatId}              : 분산 락 (Redisson)
 * - seat:hold:owner:{scheduleId}:{seatId}        : HOLD 소유자 userId (TTL 5분)
 * - seat:sold:{scheduleId}                       : SOLD 좌석 Set
 * - seat:version:{scheduleId}                    : 변경 버전 (INCR)
 * - seat:changes:{scheduleId}:{version}          : 변경 이벤트 JSON (TTL 60초)
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final RedissonClient redissonClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private final PerformanceSeatRepository performanceSeatRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final ReservationRepository reservationRepository;
    private final SeatHoldLogRepository seatHoldLogRepository;
    private final PriceGradeRepository priceGradeRepository;

    private static final String SEAT_LOCK_KEY = "seat:lock:%d:%d";
    private static final String SEAT_HOLD_OWNER_KEY = "seat:hold:owner:%d:%d";
    private static final String SEAT_SOLD_SET_KEY = "seat:sold:%d";
    private static final String SEAT_VERSION_KEY = "seat:version:%d";
    private static final String SEAT_CHANGES_KEY = "seat:changes:%d:%d";

    private static final long LOCK_WAIT_TIME = 3;        // 락 획득 대기 시간 (초)
    private static final long LOCK_LEASE_TIME = 5;       // 락 자동 해제 시간 (초)
    private static final long HOLD_TTL_SECONDS = 300;    // HOLD 만료 시간 (5분)
    private static final long CHANGES_TTL_SECONDS = 60;  // 변경 이벤트 TTL (60초)

    @Transactional
    public HoldSeatsResponse holdSeats(Long reservationId, HoldSeatsRequest request, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 예매 조회 및 검증
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ServiceException(ErrorCode.RESERVATION_NOT_FOUND));

        validateReservation(reservation, userId);
        Long scheduleId = reservation.getPerformanceScheduleId();

        // 2. 좌석별 락 획득 및 HOLD 처리
        List<Long> heldSeatIds = new ArrayList<>();
        List<RLock> acquiredLocks = new ArrayList<>();
        List<HoldSeatsFailResponse.ConflictSeat> conflicts = new ArrayList<>();

        try{
            // 2-1. 모든 좌석 락 획득 (좌석 id 정렬 후 순차적 획득)
            List<Long> sortedSeatIds = request.performanceSeatIds().stream()
                    .sorted()
                    .toList();

            for(Long seatId : sortedSeatIds){
                RLock lock = acquireSeatLock(scheduleId, seatId);
                if (lock == null) {
                    // 락 획득 실패 → conflicts에 추가하고 계속 진행
                    conflicts.add(new HoldSeatsFailResponse.ConflictSeat(
                            seatId, "UNKNOWN", "LOCK_ACQUISITION_FAILED"
                    ));
                } else {
                    acquiredLocks.add(lock);
                }
            }

            // 락 획득 실패한 좌석이 하나라도 있으면 전체 실패
            if (!conflicts.isEmpty()) {
                log.warn("[SeatHold] 락 획득 실패 - failedSeats={}",
                        conflicts.stream()
                                .map(HoldSeatsFailResponse.ConflictSeat::performanceSeatId)
                                .toList());
                throw new SeatHoldConflictException(reservationId, conflicts);
            }

            // 2-2 . 좌석 상태 검증 + HOLD 처리
            for(Long seatId : sortedSeatIds){
                try{
                    // 단일 좌석 HOLD 처리
                    holdSingleSeat(reservation, scheduleId, userId, seatId, now);
                    heldSeatIds.add(seatId);
                }catch (SeatNotAvailableException e){
                    /*
                     * 좌석 선점 불가 상황
                     * 1. 다른 사용자가 HOLD 중 (TTL 아직 만료X)
                     * 2. 이미 SOLD 상태
                     *
                     * → 전체 롤백 필요
                     */
                    conflicts.add(new HoldSeatsFailResponse.ConflictSeat(
                            e.getSeatId(),
                            e.getCurrentStatus().name(),
                            e.getReason()
                    ));
                }
            }

            // HOLD 실패한 좌석이 하나라도 있으면 롤백 후 전체 실패
            if (!conflicts.isEmpty()) {
                log.warn("[SeatHold] 좌석 HOLD 실패 - failedSeats={}",
                        conflicts.stream()
                                .map(HoldSeatsFailResponse.ConflictSeat::performanceSeatId)
                                .toList());
                rollbackHeldSeats(scheduleId, heldSeatIds, userId);
                throw new SeatHoldConflictException(reservationId, conflicts);
            }

            // 3. 예매 정보 업데이트
            updateReservation(reservation, now);

            HoldSeatsResponse response = buildHoldSeatsResponse(reservation, heldSeatIds, now);
            log.info("[SeatHold] HOLD 성공 - reservationId={}, heldSeats={}",
                    reservationId, heldSeatIds.size());

            return  response;
        } finally {
            // 4. 획득한 락 해제
            releaseLocks(acquiredLocks);
        }
    }




    // 좌석 락 획득
    private RLock acquireSeatLock(Long scheduleId, Long seatId) {
        String lockKey = String.format(SEAT_LOCK_KEY, scheduleId, seatId);
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            return acquired ? lock : null;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[SeatHold] 락 획득 중 인터럽트 - seatId={}", seatId, e);
            return null;
        }
    }

    // 단일 좌석 HOLD 처리
    private void holdSingleSeat(Reservation reservation, Long scheduleId, Long userId, Long seatId, LocalDateTime now) {
        // 1. DB에서 좌석 조회
        PerformanceSeat seat = performanceSeatRepository.findById(seatId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PERFORMANCE_SEAT_NOT_FOUND));

        // 2. Redis에서 좌석 상태 검증
        SeatStatus currentStatus = checkSeatStatusInRedis(scheduleId, seat);

        // 3. 선점 가능 여부 확인
        if (currentStatus == SeatStatus.SOLD) {
            throw new SeatNotAvailableException(seatId, currentStatus, "ALREADY_SOLD");
        }

        if (currentStatus == SeatStatus.HOLD) {
            throw new SeatNotAvailableException(seatId, currentStatus, "ALREADY_HELD");
        }

        // 4. Redis에 HOLD 소유자 기록 (TTL 5분)
        String holdOwnerKey = String.format(SEAT_HOLD_OWNER_KEY, scheduleId, seatId);
        redisTemplate.opsForValue().set(
                holdOwnerKey,
                userId.toString(),
                HOLD_TTL_SECONDS,
                TimeUnit.SECONDS
        );

        // 5. SOLD set 중복 확인
        String soldSetKey = String.format(SEAT_SOLD_SET_KEY, scheduleId);
        Boolean isSold = redisTemplate.opsForSet().isMember(soldSetKey, seatId.toString());

        if(Boolean.TRUE.equals(isSold)){
            // SOLD set에 있으면 절대 HOLD 불가
            log.error("[SeatHold] SOLD 좌석 HOLD 시도 차단 - seatId={}", seatId);
            throw new SeatNotAvailableException(seatId, SeatStatus.SOLD, "ALREADY_SOLD");
        }

        // 6. DB 상태 업데이트
        LocalDateTime expiresAt = now.plusSeconds(HOLD_TTL_SECONDS);
        seat.updateStatus(SeatStatus.HOLD);
        seat.updateHoldInfo(userId, expiresAt);
        performanceSeatRepository.save(seat);

        // 7. ReservationSeat 생성
        PriceGrade priceGrade = priceGradeRepository.findById(seat.getPriceGradeId())
                .orElseThrow(() -> new ServiceException(ErrorCode.PRICE_GRADE_NOT_FOUND));

        ReservationSeat reservationSeat = ReservationSeat.builder()
                .reservationId(reservation.getId())
                .performanceSeatId(seatId)
                .price(priceGrade.getPrice())
                .build();
        reservationSeatRepository.save(reservationSeat);

        // 8. SeatHoldLog 기록
        SeatHoldLog holdLog = SeatHoldMapper.toHoldLog(seatId, userId, now, expiresAt);
        seatHoldLogRepository.save(holdLog);

        // 9. 변경 이벤트 발행
        recordSeatChangeEvent(scheduleId, seatId, SeatStatus.HOLD, userId);
    }

    // Redis에서 좌석 상태 확인
    private SeatStatus checkSeatStatusInRedis(Long scheduleId, PerformanceSeat seat) {
        // SOLD set 체크
        String soldSetKey = String.format(SEAT_SOLD_SET_KEY, scheduleId);
        Boolean isSold = redisTemplate.opsForSet().isMember(soldSetKey, seat.getId().toString());

        if (Boolean.TRUE.equals(isSold)) {
            return SeatStatus.SOLD;
        }

        // HOLD 체크
        String holdOwnerKey = String.format(SEAT_HOLD_OWNER_KEY, scheduleId, seat.getId());
        String holdUserId = redisTemplate.opsForValue().get(holdOwnerKey);

        if (holdUserId != null) { // HOLD 소유자 키 존재 → HOLD 상태
            return SeatStatus.HOLD;
        }

        // Redis에 없으면 DB 상태 반환
        return seat.getStatus();
    }

    // 변경 이벤트 기록(폴링용)
    private void recordSeatChangeEvent(Long scheduleId, Long seatId, SeatStatus status, Long userId) {
        // 버전 증가
        String versionKey = String.format(SEAT_VERSION_KEY, scheduleId);
        Long version = redisTemplate.opsForValue().increment(versionKey);

        // 이벤트 JSON 생성
        Map<String, Object> event = new HashMap<>();
        event.put("seatId", seatId);
        event.put("status", status.name());
        event.put("userId", userId);
        event.put("timestamp", LocalDateTime.now().toString());

        // Redis 저장 (TTL 60초)
        String changeKey = String.format(SEAT_CHANGES_KEY, scheduleId, version);

        try{
            String eventJson = objectMapper.writeValueAsString(event);
            redisTemplate.opsForValue().set(
                    changeKey,
                    eventJson,
                    CHANGES_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
        }catch(JsonProcessingException e) {
            log.error("[SeatHold] 변경 이벤트 JSON 변환 실패", e);
        }
    }

    // HOLD 실패시 롤백처리
    private void rollbackHeldSeats(Long scheduleId, List<Long> seatIds, Long userId) {
        log.warn("[SeatHold] 롤백 시작 - seatCount={}", seatIds.size());

        for(Long seatId : seatIds){
            try {
                // Redis 키 삭제
                String holdOwnerKey = String.format(SEAT_HOLD_OWNER_KEY, scheduleId, seatId);
                redisTemplate.delete(holdOwnerKey);

                // DB 상태 복원
                PerformanceSeat seat = performanceSeatRepository.findById(seatId)
                        .orElseThrow(() -> new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR));

                seat.updateStatus(SeatStatus.AVAILABLE);
                seat.clearHoldInfo();
                performanceSeatRepository.save(seat);

                // ReservationSeat 삭제
                // TODO: reservationId로 필터링 필요

                // 롤백 로그 기록
                SeatHoldLog rollbackLog = SeatHoldMapper.toReleaseLog(seatId, userId);
                seatHoldLogRepository.save(rollbackLog);

            } catch (Exception e) {
                log.error("[SeatHold] 롤백 실패 - seatId={}", seatId, e);
            }
        }
    }

    // 예매 정보 업데이트
    private void updateReservation(Reservation reservation, LocalDateTime now) {
        // 상태, 만료 시간 업데이트
        LocalDateTime expiresAt = now.plusSeconds(HOLD_TTL_SECONDS);
        reservation.updateStatus(ReservationStatus.HOLD);
        reservation.updateExpiresAt(expiresAt);

        // 총액 계산
        List<ReservationSeat> reservationSeats = reservationSeatRepository.findByReservationId(reservation.getId());
        int totalAmount = reservationSeats.stream()
                .mapToInt(ReservationSeat::getPrice)
                .sum();

        reservation.updateTotalAmount(totalAmount);
        reservationRepository.save(reservation);
    }

    // HOLD 성공 응답 생성
    private HoldSeatsResponse buildHoldSeatsResponse(Reservation reservation, List<Long> heldSeatIds, LocalDateTime now) {
        List<ReservationSeat> reservationSeats = reservationSeatRepository.findByReservationId(reservation.getId());

        Map<Long, PerformanceSeat> seatMap = performanceSeatRepository.findAllById(heldSeatIds)
                .stream()
                .collect(Collectors.toMap(PerformanceSeat::getId, seat -> seat));

        Set<Long> priceGradeIds = seatMap.values().stream()
                .map(PerformanceSeat::getPriceGradeId)
                .collect(Collectors.toSet());

        Map<Long, PriceGrade> priceGradeMap = priceGradeRepository.findAllById(priceGradeIds)
                .stream()
                .collect(Collectors.toMap(PriceGrade::getId, grade -> grade));

        return ReservationMapper.toHoldSeatsResponse(
                reservation,
                reservationSeats,
                seatMap,
                priceGradeMap,
                now
        );
    }

    // 락 해제
    private void releaseLocks(List<RLock> locks) {
        for(RLock lock : locks){
            try{
                if(lock != null && lock.isHeldByCurrentThread()){
                    lock.unlock();
                }
            }catch (Exception e){
                log.error("[SeatHold] 락 해제 실패", e);
            }
        }
    }

    // 예매 검증
    private void validateReservation(Reservation reservation, Long userId) {
        // 소유자 확인
        if (!reservation.isOwnedBy(userId)) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED);
        }

        // 결제 완료 확인
        if (reservation.getStatus() == ReservationStatus.PAID) {
            throw new ServiceException(ErrorCode.ALREADY_PAID_RESERVATION);
        }

        // 만료 확인
        if (reservation.getStatus() == ReservationStatus.EXPIRED || reservation.isExpired()) {
            throw new ServiceException(ErrorCode.RESERVATION_EXPIRED);
        }
    }

    // 변경 이벤트 조회 (폴링 API)
    public List<Map<String, Object>> getSeatChanges(Long scheduleId, Long sinceVersion){
        String versionKey = String.format(SEAT_VERSION_KEY, scheduleId);
        String currentVersionStr = redisTemplate.opsForValue().get(versionKey);

        if(currentVersionStr == null || sinceVersion >= Long.parseLong(currentVersionStr)){
            return List.of(); // 버전 키 없음 = 변경 없음
        }

        Long currentVersion = Long.parseLong(currentVersionStr);
        List<Map<String, Object>> changes = new ArrayList<>();

        // sinceVersion+1 부터 현재 버전까지 변경 이벤트 조회
        for(long v = sinceVersion + 1; v <= currentVersion; v++){
            String changeKey = String.format(SEAT_CHANGES_KEY, scheduleId, v);
            String eventJson = redisTemplate.opsForValue().get(changeKey);

            if(eventJson != null){
                try{
                    @SuppressWarnings("unchecked")
                    Map<String, Object> event = objectMapper.readValue(eventJson, Map.class);
                    event.put("version", v);
                    changes.add(event);
                }catch(JsonProcessingException e){
                    log.error("[SeatHold] 변경 이벤트 JSON 파싱 실패 - version={}", v, e);
                }
            }
        }

        return changes;
    }
}
