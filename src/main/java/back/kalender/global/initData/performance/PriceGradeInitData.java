package back.kalender.global.initData.performance;

import back.kalender.domain.performance.performance.entity.Performance;
import back.kalender.domain.performance.performance.repository.PerformanceRepository;
import back.kalender.domain.performance.priceGrade.entity.PriceGrade;
import back.kalender.domain.performance.priceGrade.repository.PriceGradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
public class PriceGradeInitData implements ApplicationRunner {

    private final PerformanceRepository performanceRepository;
    private final PriceGradeRepository priceGradeRepository;

    @Override
    public void run(ApplicationArguments args) {

        if (priceGradeRepository.count() > 0) return;

        for (Performance p : performanceRepository.findAll()) {
            save(p, "VIP", 180000);
            save(p, "R", 150000);
            save(p, "S", 120000);
            save(p, "A", 90000);
        }
    }

    private void save(Performance p, String name, int price) {
        priceGradeRepository.save(
            new PriceGrade(p.getId(), name, price)
        );
    }
}