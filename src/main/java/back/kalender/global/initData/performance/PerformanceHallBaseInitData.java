package back.kalender.global.initData.performance;

import back.kalender.domain.performance.performanceHall.entity.PerformanceHall;
import back.kalender.domain.performance.performanceHall.repository.PerformanceHallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@Profile("dev")
@Order(0)
@RequiredArgsConstructor
public class PerformanceHallBaseInitData implements ApplicationRunner {

    private final PerformanceHallRepository performanceHallRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (performanceHallRepository.count() > 0) return;

        performanceHallRepository.save(
                new PerformanceHall(
                        "KPOP Arena Hall",
                        "서울 송파구 올림픽로 25",
                        "지하철 2호선 ○○역 4번출구 도보 5분"
                )
        );

        System.out.println("✅ PerformanceHall 초기 데이터 생성 완료");
    }
}
