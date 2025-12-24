package back.kalender.domain.performance.priceGrade.entity;

import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "price_grades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PriceGrade extends BaseEntity {
    @Column(name = "performance_id", nullable = false)
    private Long performanceId;

    @Column(name = "grade_name")
    private String gradeName;

    private Integer price;
}
