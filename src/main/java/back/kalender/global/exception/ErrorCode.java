package back.kalender.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Common 0
    BAD_REQUEST("001", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."), // 예시, 삭제가능
    UNAUTHORIZED("002",HttpStatus.UNAUTHORIZED,"로그인이 필요합니다."),
    INTERNAL_SERVER_ERROR("003",HttpStatus.INTERNAL_SERVER_ERROR,"서버에서 오류가 발생했습니다."),

    // User 1000
    USER_NOT_FOUND("1001", HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),

    // Artist 2000
    ARTIST_NOT_FOUND("2001",HttpStatus.NOT_FOUND,"아티스트를 찾을 수 없습니다."),
    ALREADY_FOLLOWED("2002",HttpStatus.CONFLICT,"이미 팔로우한 아티스트입니다."),
    ARTIST_NOT_FOLLOWED("2003", HttpStatus.BAD_REQUEST, "팔로우 상태가 아닙니다.");

    // Party 3000

    // Schedule 4000

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
