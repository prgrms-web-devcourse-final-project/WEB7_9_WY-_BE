package back.kalender.global.config;

import back.kalender.domain.booking.web.BookingAccessInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final BookingAccessInterceptor bookingAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(bookingAccessInterceptor)
                .addPathPatterns("/api/v1/booking/**")
                // 마이페이지 예매내역 조회는 제외
                .excludePathPatterns(
                        "/api/v1/booking/my-reservations",
                        "/api/v1/booking/reservation/*"
                );
    }
}
