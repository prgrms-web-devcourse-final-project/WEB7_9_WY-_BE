package back.kalender.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Common 0
    BAD_REQUEST("001", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."), // 예시, 삭제가능

    // User 1000
    USER_NOT_FOUND("1001", HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),

    // Artist 2000

    // Party 3000

    // Schedule 4000
    SCHEDULE_NOT_FOUND("4001", HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다.");

    // Performance 5000

    // MyPage 6000

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
