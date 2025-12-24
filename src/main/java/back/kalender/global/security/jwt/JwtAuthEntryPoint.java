package back.kalender.global.security.jwt;

import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        
        // 이미 응답이 커밋되었거나 다른 핸들러가 처리한 경우 무시
        // (예: GlobalExceptionHandler가 이미 비즈니스 예외를 처리한 경우)
        if (response.isCommitted()) {
            return;
        }

        // SecurityContext에 Authentication이 있는 경우, 인증은 성공한 상태이므로
        // 이는 실제 인증 실패가 아닌 다른 예외(비즈니스 예외 등)일 가능성이 높습니다.
        // GlobalExceptionHandler가 처리하도록 두고, 여기서는 처리하지 않습니다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            // 인증은 성공했지만 다른 예외가 발생한 경우
            // GlobalExceptionHandler가 처리하도록 예외를 다시 던지지 않고 무시
            // (실제로는 Spring Security가 이미 예외를 처리했을 것이므로 여기서는 응답을 보내지 않음)
            return;
        }

        // 실제 인증 실패인 경우에만 401 응답 반환
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        // ErrorResponse 형식으로 통일
        ErrorResponse errorResponse = ErrorResponse.errorResponse(ErrorCode.UNAUTHORIZED);
        String json = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(json);
    }
}
