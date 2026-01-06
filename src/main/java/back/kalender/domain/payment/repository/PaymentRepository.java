package back.kalender.domain.payment.repository;

import back.kalender.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByUserIdAndReservationIdAndIdempotencyKey(Long userId, Long reservationId, String idempotencyKey);

    Optional<Payment> findByUserIdAndReservationId(Long userId, Long reservationId);
    
    // 같은 userId와 reservationId로 생성된 모든 Payment 조회 (예약당 결제 하나만 보장용)
    @Query("SELECT p FROM Payment p WHERE p.userId = :userId AND p.reservationId = :reservationId ORDER BY p.id DESC")
    java.util.List<Payment> findAllByUserIdAndReservationId(@Param("userId") Long userId, @Param("reservationId") Long reservationId);

    Optional<Payment> findByPaymentKey(String paymentKey);

    // 조건부 UPDATE: CREATED → PROCESSING (금액 검증 포함)
    @Modifying
    @Query("UPDATE Payment p SET p.status = 'PROCESSING' WHERE p.id = :id AND p.status = 'CREATED' AND p.amount = :amount")
    int updateStatusToProcessing(@Param("id") Long id, @Param("amount") Integer amount);

    // 조건부 UPDATE: PROCESSING → APPROVED
    @Modifying
    @Query("UPDATE Payment p SET p.status = 'APPROVED', p.paymentKey = :paymentKey, p.orderId = :orderId, p.approvedAt = :approvedAt " +
           "WHERE p.id = :id AND p.status = 'PROCESSING'")
    int updateStatusToApproved(@Param("id") Long id, @Param("paymentKey") String paymentKey, @Param("orderId") String orderId, @Param("approvedAt") LocalDateTime approvedAt);

    // 조건부 UPDATE: APPROVED → CANCELED
    @Modifying
    @Query("UPDATE Payment p SET p.status = 'CANCELED', p.canceledAt = :canceledAt " +
           "WHERE p.id = :id AND p.status = 'APPROVED'")
    int updateStatusToCanceled(@Param("id") Long id, @Param("canceledAt") LocalDateTime canceledAt);

    // 조건부 UPDATE: PROCESSING → FAILED
    @Modifying
    @Query("UPDATE Payment p SET p.status = 'FAILED', p.failCode = :failCode, p.failMessage = :failMessage " +
           "WHERE p.id = :id AND p.status = 'PROCESSING'")
    int updateStatusToFailed(@Param("id") Long id, @Param("failCode") String failCode, @Param("failMessage") String failMessage);

    // 조건부 UPDATE: PROCESSING → CREATED (게이트웨이 호출 실패 시 롤백)
    @Modifying
    @Query("UPDATE Payment p SET p.status = 'CREATED' WHERE p.id = :id AND p.status = 'PROCESSING'")
    int updateStatusToCreated(@Param("id") Long id);

    // 조건부 UPDATE: PROCESSING → PROCESSING_TIMEOUT
    @Modifying
    @Query("UPDATE Payment p SET p.status = 'PROCESSING_TIMEOUT' WHERE p.id = :id AND p.status = 'PROCESSING'")
    int updateStatusToTimeout(@Param("id") Long id);

    // 복구 워커용: PROCESSING/PROCESSING_TIMEOUT 상태가 일정 시간 이상 지속되는 결제 조회
    @Query("SELECT p FROM Payment p WHERE p.status IN ('PROCESSING', 'PROCESSING_TIMEOUT') " +
           "AND p.updatedAt < :beforeTime ORDER BY p.updatedAt ASC")
    java.util.List<Payment> findStuckProcessingPayments(@Param("beforeTime") LocalDateTime beforeTime);
}

