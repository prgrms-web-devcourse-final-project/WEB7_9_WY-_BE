package back.kalender.domain.performance.performanceHall.controller;

import back.kalender.domain.performance.performanceHall.service.PerformanceHallService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/performanceHall")
public class PerformanceHallController {

    private final PerformanceHallService performanceHallService;


}
