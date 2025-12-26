package back.kalender.domain.booking.seatHold.service;

import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.booking.reservation.dto.request.HoldSeatsRequest;
import back.kalender.domain.booking.reservation.dto.request.ReleaseSeatsRequest;
import back.kalender.domain.booking.reservation.dto.response.HoldSeatsFailResponse;
import back.kalender.domain.booking.reservation.dto.response.HoldSeatsResponse;
import back.kalender.domain.booking.reservation.dto.response.ReleaseSeatsResponse;
import back.kalender.domain.booking.reservation.entity.Reservation;
import back.kalender.domain.booking.reservation.entity.ReservationStatus;
import back.kalender.domain.booking.reservation.mapper.ReservationMapper;
import back.kalender.domain.booking.reservation.repository.ReservationRepository;
import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;
import back.kalender.domain.booking.reservationSeat.mapper.ReservationSeatMapper;
import back.kalender.domain.booking.reservationSeat.repository.ReservationSeatRepository;
import back.kalender.domain.booking.seatHold.entity.SeatHoldLog;
import back.kalender.domain.booking.seatHold.event.SeatHoldCompletedEvent;
import back.kalender.domain.booking.seatHold.event.SeatReleaseCompletedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 좌석 홀드, 해제 (이벤트 기반)
 * <Redis 키 설계>
 * - seat:lock:{scheduleId}:{seatId}              : 분산 락 (Redisson)
 * - seat:hold:owner:{scheduleId}:{seatId}        : HOLD 소유자 userId (TTL 7분)
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
    private final ApplicationEventPublisher eventPublisher;

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
    private static final long HOLD_TTL_SECONDS = 420;    // HOLD 만료 시간 (7분)
    private static final long CHANGES_TTL_SECONDS = 60;  // 변경 이벤트 TTL (60초)

    @Transactional
    public HoldSeatsResponse holdSeats(Long reservationId, HoldSeatsRequest request, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        // 예매 조회 및 검증
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ServiceException(ErrorCode.RESERVATION_NOT_FOUND));

        validateReservation(reservation, userId);
        Long scheduleId = reservation.getPerformanceScheduleId();

        // 좌석별 락 획득 및 HOLD 처리
        List<Long> heldSeatIds = new ArrayList<>();
        List<RLock> acquiredLocks = new ArrayList<>();
        List<HoldSeatsFailResponse.ConflictSeat> conflicts = new ArrayList<>();

        try{
            // 모든 좌석 락 획득 (좌석 id 정렬 후 순차적 획득)
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

            // 좌석 상태 검증 + HOLD 처리 (DB만)
            for(Long seatId : sortedSeatIds){
                try{
                    //DB 작업만 수행, Redis는 이벤트로 처리
                    holdSingleSeatInDB(reservation, scheduleId, userId, seatId, now);
                    heldSeatIds.add(seatId);
                }catch (SeatNotAvailableException e){
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
                rollbackHeldSeats(reservation.getId(), scheduleId, heldSeatIds, userId);
                throw new SeatHoldConflictException(reservationId, conflicts);
            }

            // 예매 정보 업데이트
            updateReservation(reservation, now);

            HoldSeatsResponse response = buildHoldSeatsResponse(reservation, heldSeatIds, now);
            log.info("[SeatHold] HOLD 성공 - reservationId={}, heldSeats={}",
                    reservationId, heldSeatIds.size());

            return  response;
        } finally {
            // 획득한 락 해제
            releaseLocks(acquiredLocks);
        }
    }

    // 단일 좌석 HOLD 처리 (DB 작업만)
    private void holdSingleSeatInDB(Reservation reservation, Long scheduleId, Long userId, Long seatId, LocalDateTime now) {
        // DB에서 좌석 조회
        PerformanceSeat seat = performanceSeatRepository.findByIdAndScheduleId(seatId, scheduleId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PERFORMANCE_SEAT_NOT_FOUND));

        // SOLD 여부 우선 체크
        String soldSetKey = String.format(SEAT_SOLD_SET_KEY, scheduleId);
        Boolean isSold = redisTemplate.opsForSet().isMember(soldSetKey, seatId.toString());
        if(Boolean.TRUE.equals(isSold) || seat.getStatus() == SeatStatus.SOLD){
            log.error("[SeatHold] SOLD 좌석 HOLD 시도 차단 - seatId={}", seatId);
            throw new SeatNotAvailableException(seatId, SeatStatus.SOLD, "ALREADY_SOLD");
        }

        // Redis에 HOLD owner 체크 (중복 홀드 방지)
        String holdOwnerKey = String.format(SEAT_HOLD_OWNER_KEY, scheduleId, seatId);
        String redisOwner = redisTemplate.opsForValue().get(holdOwnerKey);

        // Redis Owner는 없는데 DB가 HOLD인 경우 - 만료 여부로 정리
        if(redisOwner == null && seat.getStatus() == SeatStatus.HOLD){
            if(seat.isHoldExpired(now)){
                log.warn("[SeatHold] Redis Onwer 없음, DB Hold 만료 -> AVAILABLE로 복구, seatId={}", seatId );
                seat.updateStatus(SeatStatus.AVAILABLE);
                seat.clearHoldInfo();
                performanceSeatRepository.save(seat);

                // 폴링 반영 위한 복구 이벤트
                eventPublisher.publishEvent(new SeatReleaseCompletedEvent(scheduleId, seatId, 0L, SeatStatus.AVAILABLE));
            }else{
                log.warn("[SeatHold] Redis Onwer 없음, DB Hold 만료X seatId={}", seatId );
                throw new SeatNotAvailableException(seatId, SeatStatus.HOLD, "INCONSISTENT_STATE");
            }
        }

        // Redis에 Owner가 있으면 이미 누군가 홀드중
        if (redisOwner != null) {
            throw new SeatNotAvailableException(seatId, SeatStatus.HOLD, "ALREADY_HELD");
        }

        // db기준 AVAILABLE인 좌석만 홀드 가능
        if(seat.getStatus() != SeatStatus.AVAILABLE){
            throw new SeatNotAvailableException(seatId, seat.getStatus(), "NOT_AVAILABLE");
        }

        // 6. DB 상태 업데이트
        LocalDateTime expiresAt = now.plusSeconds(HOLD_TTL_SECONDS);
        seat.updateStatus(SeatStatus.HOLD);
        seat.updateHoldInfo(userId, expiresAt);
        performanceSeatRepository.save(seat);

        // ReservationSeat 생성
        PriceGrade priceGrade = priceGradeRepository.findById(seat.getPriceGradeId())
                .orElseThrow(() -> new ServiceException(ErrorCode.PRICE_GRADE_NOT_FOUND));

        ReservationSeat reservationSeat = ReservationSeatMapper.create(
                reservation.getId(),
                seatId,
                priceGrade.getPrice()
        );
        reservationSeatRepository.save(reservationSeat);

        // SeatHoldLog 기록
        SeatHoldLog holdLog = SeatHoldMapper.toHoldLog(seatId, userId, now, expiresAt);
        seatHoldLogRepository.save(holdLog);

        // 변경 이벤트 발행
        eventPublisher.publishEvent(
                new SeatHoldCompletedEvent(
                        scheduleId,
                        seatId,
                        userId,
                        SeatStatus.HOLD,
                        HOLD_TTL_SECONDS
                )
        );

        log.debug("[SeatHold] DB 작업 완료, 이벤트 발행 - seatId={}, userId={}", seatId, userId);
    }

    // HOLD 실패시 롤백처리
    private void rollbackHeldSeats(Long reservationId, Long scheduleId, List<Long> seatIds, Long userId) {
        if (seatIds == null || seatIds.isEmpty()) return;
        log.warn("[SeatHold] 롤백 시작 - seatCount={}", seatIds.size());

        try {
            reservationSeatRepository.deleteByReservationIdAndPerformanceSeatIdIn(reservationId, seatIds);
        } catch (Exception e) {
            log.error("[SeatHold] 롤백 중 ReservationSeat 삭제 실패 - reservationId={}", reservationId, e);
        }

        for(Long seatId : seatIds){
            try {
                log.debug("[SeatRelease] 좌석 해제 시도 - seatId={}", seatId);
                // Redis 키 삭제
                String holdOwnerKey = String.format(SEAT_HOLD_OWNER_KEY, scheduleId, seatId);
                redisTemplate.delete(holdOwnerKey);

                // DB 상태 복원
                PerformanceSeat seat = performanceSeatRepository.findByIdAndScheduleId(seatId, scheduleId)
                        .orElseThrow(() -> new ServiceException(ErrorCode.PERFORMANCE_SEAT_NOT_FOUND));

                seat.updateStatus(SeatStatus.AVAILABLE);
                seat.clearHoldInfo();
                performanceSeatRepository.save(seat);

                // 롤백 로그 기록
                SeatHoldLog rollbackLog = SeatHoldMapper.toReleaseLog(seatId, userId);
                seatHoldLogRepository.save(rollbackLog);

                // 변경 이벤트 발행
                eventPublisher.publishEvent(
                        new SeatReleaseCompletedEvent(scheduleId, seatId, userId, SeatStatus.AVAILABLE)
                );

            } catch (Exception e) {
                log.error("[SeatHold] 롤백 실패 - seatId={}", seatId, e);
            }
        }
    }

    // 좌석 RELEASE
    @Transactional
    public ReleaseSeatsResponse releaseSeats(Long reservationId, ReleaseSeatsRequest request, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        // 예매 조회 및 검증
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ServiceException(ErrorCode.RESERVATION_NOT_FOUND));

        validateReservation(reservation, userId);
        Long scheduleId = reservation.getPerformanceScheduleId();

        // 전체 해제 여부 검증
        List<ReservationSeat> allReservationSeats = reservationSeatRepository
                .findByReservationId(reservationId);

        if(request.performanceSeatIds().size() != allReservationSeats.size()){
            log.error("[SeatHold] 전체 해제가 아닌 요청 - reservationId={}, total={}, requested={}",
                    reservationId, allReservationSeats.size(), request.performanceSeatIds().size());
            throw new ServiceException(ErrorCode.PARTIAL_RELEASE_NOT_ALLOWED);
        }

        // 요청딘 좌석 id가 실제 예매의 좌석 id와 일치하는지 검증
        Set <Long> reservedSeatIds = allReservationSeats.stream()
                .map(ReservationSeat::getPerformanceSeatId)
                .collect(Collectors.toSet());
        Set<Long> requestedSeatIds = new HashSet<>(request.performanceSeatIds());

        if (!reservedSeatIds.equals(requestedSeatIds)) {
            log.error("[SeatHold] 좌석 ID 불일치 - reservationId={}", reservationId);
            throw new ServiceException(ErrorCode.BAD_REQUEST);
        }

        log.info("[SeatHold] 전체 해제 검증 완료 - seatCount={}", allReservationSeats.size());

        List<Long> sortedSeatIds = request.performanceSeatIds().stream()
                .sorted()
                .toList();

        // 좌석별 락 획득 및 RELEASE 처리
        List<RLock> acquiredLocks = new ArrayList<>();

        try {
            // 모든 좌석 락 획득
            for (Long seatId : sortedSeatIds) {
                RLock lock = acquireSeatLock(scheduleId, seatId);

                if (lock == null) {
                    log.error("[SeatHold] 락 획득 실패 - seatId={}", seatId);
                    throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR);
                }
                acquiredLocks.add(lock);
            }

            // 좌석 RELEASE 처리 (db만)
            for (Long seatId : sortedSeatIds) {
                releaseSingleSeatInDB(scheduleId, userId, seatId, now);
            }
            reservationSeatRepository.deleteByReservationId(reservationId);

            // 예매 상태 업데이트
            reservation.cancel();
            reservationRepository.save(reservation);

            // 응답 생성
            ReleaseSeatsResponse response = ReservationMapper.toReleaseSeatsResponse(
                    reservation,
                    sortedSeatIds,
                    0,
                    0,
                    now
            );
            log.info("[SeatHold] RELEASE 성공 - reservationId={}", reservationId);

            return response;
        }finally {
            // 획득한 락 해제
            releaseLocks(acquiredLocks);
        }

    }

    // 단일 좌석 RELEASE 처리 (db 작업만 수행)
    private void releaseSingleSeatInDB(Long scheduleId, Long userId, Long seatId, LocalDateTime now) {
        // DB에서 좌석 조회
        PerformanceSeat seat = performanceSeatRepository.findByIdAndScheduleId(seatId, scheduleId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PERFORMANCE_SEAT_NOT_FOUND));

        // SOLD 좌석은 RELEASE 불가
        if(seat.getStatus() == SeatStatus.SOLD){
            return;
        }

        // 권한 검증 (owner 확인)
        String holdOwnerKey = String.format(SEAT_HOLD_OWNER_KEY, scheduleId, seatId);
        String currentOwner = redisTemplate.opsForValue().get(holdOwnerKey);

        // TTL 만료로 Redis owner없으면 db상태만 정리
        if(currentOwner == null){
            // 이미 AVAILABLE이면 그냥 성공
            if (seat.getStatus() == SeatStatus.AVAILABLE) return;

            // HOLD거나 기타 상태면 강제 복구
            seat.updateStatus(SeatStatus.AVAILABLE);
            seat.clearHoldInfo();
            performanceSeatRepository.save(seat);

            // 폴링 반영(복구 이벤트)
            eventPublisher.publishEvent(
                    new SeatReleaseCompletedEvent(scheduleId, seatId, 0L, SeatStatus.AVAILABLE)
            );
            return;
        }

        if (!userId.toString().equals(currentOwner)) {
            // 다른사용자가 이미 선점
            log.error("[SeatHold] RELEASE 권한 없음 - seatId={}, owner={}, userId={}",
                    seatId, currentOwner, userId);
            throw new ServiceException(ErrorCode.UNAUTHORIZED);
        }

        // DB 상태 업데이트
        seat.updateStatus(SeatStatus.AVAILABLE);
        seat.clearHoldInfo();
        performanceSeatRepository.save(seat);

        // SeatHoldLog 기록
        SeatHoldLog releaseLog = SeatHoldMapper.toReleaseLog(seatId, userId);
        seatHoldLogRepository.save(releaseLog);

        // RELEASE 완료 이벤트 발행
        eventPublisher.publishEvent(new SeatReleaseCompletedEvent(scheduleId, seatId, userId, SeatStatus.AVAILABLE));

        log.debug("[SeatHold] DB RELEASE 완료, 이벤트 발행 - seatId={}, userId={}", seatId, userId);
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

        // 버전 차이가 너무 크면 전체 새로고침 유도
        long versionGap = currentVersion - sinceVersion;
        if (versionGap > 100) {
            log.warn("[SeatChange] 버전 차이 너무 큼 - gap={}, scheduleId={}",
                    versionGap, scheduleId);

            // 프론트에 전체 좌석표 재조회 요청
            Map<String, Object> refreshEvent = Map.of(
                    "type", "FULL_REFRESH_REQUIRED",
                    "currentVersion", currentVersion,
                    "message", "Too many changes. Please refresh seat layout."
            );
            return List.of(refreshEvent);
        }

        // 정상 범위: 최대 100개만 조회
        List<Map<String, Object>> changes = new ArrayList<>();
        long startVersion = sinceVersion + 1;
        long endVersion = Math.min(currentVersion, sinceVersion + 100);

        for (long v = startVersion; v <= endVersion; v++) {
            String changeKey = String.format(SEAT_CHANGES_KEY, scheduleId, v);
            String eventJson = redisTemplate.opsForValue().get(changeKey);

            if (eventJson != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> event = objectMapper.readValue(
                            eventJson,
                            Map.class
                    );
                    event.put("version", v);
                    changes.add(event);
                } catch (JsonProcessingException e) {
                    log.error("[SeatChange] JSON 파싱 실패 - version={}", v, e);
                }
            }
            // eventJson이 null이면 TTL 만료 → 스킵
        }

        return changes;
    }

}
