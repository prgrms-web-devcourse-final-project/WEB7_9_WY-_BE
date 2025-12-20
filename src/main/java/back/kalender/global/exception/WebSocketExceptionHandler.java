package back.kalender.global.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * WebSocket 메시지 처리 중 발생하는 예외를 처리하는 핸들러
 * @MessageMapping 메서드에서 발생한 예외를 처리합니다.
 *
 * HTTP 예외 처리(GlobalExceptionHandler)와는 별도로 동작합니다.
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class WebSocketExceptionHandler {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * ServiceException 처리
     * 채팅 관련 비즈니스 로직 예외
     */
    @MessageExceptionHandler(ServiceException.class)
    public void handleServiceException(
            ServiceException e,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        ErrorCode errorCode = e.getErrorCode();
        String userName = headerAccessor.getUser() != null
                ? headerAccessor.getUser().getName()
                : "unknown";

        log.error("WebSocket ServiceException - code: {}, message: {}, user: {}",
                errorCode.getCode(),
                errorCode.getMessage(),
                userName);

        // 로그만 남기고 클라이언트에는 전송하지 않음
        // 클라이언트는 타임아웃이나 재연결로 대응
    }

    /**
     * 일반 예외 처리
     * 예상하지 못한 예외 발생 시
     */
    @MessageExceptionHandler(Exception.class)
    public void handleException(
            Exception e,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String userName = headerAccessor.getUser() != null
                ? headerAccessor.getUser().getName()
                : "unknown";

        log.error("WebSocket 예외 발생 - message: {}, user: {}",
                e.getMessage(),
                userName,
                e);

        // 일반 예외는 로그만 남김
    }

    /**
     * WebSocket 에러 응답 DTO
     */
    private record WebSocketErrorResponse(
            String code,
            String message
    ) {}
}