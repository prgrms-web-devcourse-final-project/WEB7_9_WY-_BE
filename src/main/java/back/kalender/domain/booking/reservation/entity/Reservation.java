package back.kalender.domain.booking.reservation.entity;

import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Reservation extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "schedule_id", nullable = false)
    private Long performanceScheduleId;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    private LocalDateTime expiresAt;

    private LocalDateTime confirmedAt;

    private Long remainingSeconds;

    @Column(name = "delivery_method", length = 20)
    private String deliveryMethod;  // DELIVERY, PICKUP

    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "recipient_address", length = 500)
    private String recipientAddress;

    @Column(name = "recipient_zip_code", length = 10)
    private String recipientZipCode;

    public static Reservation create(Long userId, Long scheduleId) {
        LocalDateTime now = LocalDateTime.now();
        return Reservation.builder()
                .userId(userId)
                .performanceScheduleId(scheduleId)
                .totalAmount(0)
                .status(ReservationStatus.HOLD)
                .expiresAt(now.plusMinutes(5))
                .build();
    }

    // 배송정보 업데이트
    public void updateDeliveryInfo(
            String deliveryMethod,
            String recipientName,
            String recipientPhone,
            String recipientAddress,
            String recipientZipCode
    ) {
        this.deliveryMethod = deliveryMethod;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.recipientAddress = recipientAddress;
        this.recipientZipCode = recipientZipCode;
    }

    // --- 상태 변경 메서드 ---

    // 예매 상태 변경
    public void updateStatus(ReservationStatus status) {
        this.status = status;
    }

    // 만료 시간 업데이트
    public void updateExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    // 총액 업데이트
    public void updateTotalAmount(Integer totalAmount) {
        this.totalAmount = totalAmount;
    }

    // 예매 취소
    public void cancel(){
        this.status = ReservationStatus.CANCELLED;
        //this.cancelledAt() = LocalDateTime.now();
    }

    // --- 검증 메서드 ---

    // 만료 시간 체크
    public boolean isExpired() {
        // expiresAt이 null이면 아직 HOLD 전 (PENDING 상태)
        if (this.expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
    // 배송 정보 입력 완료 여부
    public boolean hasDeliveryInfo() {
        if ("PICKUP".equals(deliveryMethod)) {
            // 현장 수령은 이름, 전화번호만 필수
            return recipientName != null && recipientPhone != null;
        } else if ("DELIVERY".equals(deliveryMethod)) {
            // 배송은 주소까지 필수
            return recipientName != null
                    && recipientPhone != null
                    && recipientAddress != null
                    && recipientZipCode != null;
        }
        return false;
    }

    // 예매 소유자 확인
    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }
}
