package back.kalender.domain.performance.performanceHall.entity;

import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "performance_halls")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PerformanceHall extends BaseEntity {
    private String name;

    private String address;

    @Column(name = "transportation_info", columnDefinition = "TEXT")
    private String transportationInfo;
}
