package back.kalender.global.config;

import back.kalender.domain.booking.session.interceptor.BookingSessionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final BookingSessionInterceptor bookingSessionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(bookingSessionInterceptor)
                .addPathPatterns(
                        "/api/v1/booking/schedule/*/reservation",
                        "/api/v1/booking/reservation/*/seats:hold",
                        "/api/v1/booking/reservation/*/seats:release",
                        "/api/v1/booking/reservation/*/summary",
                        "/api/v1/booking/reservation/*/delivery",
                        "/api/v1/booking/schedule/*/seats/changes"
                );
    }
}