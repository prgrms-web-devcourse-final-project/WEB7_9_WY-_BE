package back.kalender.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Common 0
    INVALID_INPUT_VALUE("004", HttpStatus.BAD_REQUEST, "유효하지 않은 입력 값입니다."),
    UNAUTHORIZED("002",HttpStatus.UNAUTHORIZED,"로그인이 필요합니다."),
    INTERNAL_SERVER_ERROR("003",HttpStatus.INTERNAL_SERVER_ERROR,"서버에서 오류가 발생했습니다."),
    BAD_REQUEST("001", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    // User 1000
    USER_NOT_FOUND("1001", HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    DUPLICATE_NICKNAME("1002", HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    DUPLICATE_EMAIL("1003", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),

    // Artist 2000
    ARTIST_NOT_FOUND("2001",HttpStatus.NOT_FOUND,"아티스트를 찾을 수 없습니다."),
    ALREADY_FOLLOWED("2002",HttpStatus.CONFLICT,"이미 팔로우한 아티스트입니다."),
    ARTIST_NOT_FOLLOWED("2003", HttpStatus.BAD_REQUEST, "팔로우 상태가 아닙니다."),

    // Party 3000
    // 3001-3099: 파티 기본 CRUD
    PARTY_NOT_FOUND("3001", HttpStatus.NOT_FOUND, "파티를 찾을 수 없습니다."),

    // 3100-3199: 파티 권한
    UNAUTHORIZED_PARTY_ACCESS("3101", HttpStatus.FORBIDDEN, "파티에 접근할 권한이 없습니다."),
    UNAUTHORIZED_PARTY_LEADER("3102", HttpStatus.FORBIDDEN, "파티장만 이 작업을 수행할 수 있습니다."),
    CANNOT_MODIFY_PARTY_NOT_LEADER("3104", HttpStatus.FORBIDDEN, "파티장만 파티를 수정할 수 있습니다."),
    CANNOT_DELETE_PARTY_NOT_LEADER("3105", HttpStatus.FORBIDDEN, "파티장만 파티를 삭제할 수 있습니다."),

    // 3200-3299: 파티 신청 관련
    APPLICATION_NOT_FOUND("3201", HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."),
    ALREADY_APPLIED("3202", HttpStatus.BAD_REQUEST, "이미 신청한 파티입니다."),
    ALREADY_MEMBER("3203", HttpStatus.BAD_REQUEST, "이미 참여중인 파티입니다."),
    CANNOT_APPLY_OWN_PARTY("3204", HttpStatus.BAD_REQUEST, "본인이 만든 파티에는 신청할 수 없습니다."),
    PARTY_FULL("3205", HttpStatus.BAD_REQUEST, "파티 인원이 가득 찼습니다."),
    APPLICATION_ALREADY_PROCESSED("3206", HttpStatus.BAD_REQUEST, "이미 처리된 신청입니다."),
    CANNOT_CANCEL_APPROVED_APPLICATION("3209", HttpStatus.BAD_REQUEST, "승인된 신청은 취소할 수 없습니다."),
    PARTY_NOT_RECRUITING("3212", HttpStatus.BAD_REQUEST, "모집중인 파티가 아닙니다."),

    // 3400-3499: 파티 유효성 검증
    CANNOT_REDUCE_MAX_MEMBERS("3413", HttpStatus.BAD_REQUEST, "현재 인원보다 적게 최대 인원을 설정할 수 없습니다."),
    // Schedule 4000
    SCHEDULE_NOT_FOUND("4001", HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."),


    // Performance 5000
    PERFORMANCE_NOT_FOUND("5001", HttpStatus.NOT_FOUND, "공연을 찾을 수 없습니다."),
    PERFORMANCE_HALL_NOT_FOUND("5002", HttpStatus.NOT_FOUND, "공연장을 찾을 수 없습니다."),
    PRICE_GRADE_NOT_FOUND("5003", HttpStatus.NOT_FOUND, "가격 등급을 찾을 수 없습니다."),

    // MyPage 6000

    // Auth 7000
    INVALID_CREDENTIALS("7001", HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN("7002", HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token입니다."),
    EXPIRED_REFRESH_TOKEN("7003", HttpStatus.UNAUTHORIZED, "만료된 refresh token입니다."),
    INVALID_EMAIL_VERIFICATION_CODE("7004", HttpStatus.BAD_REQUEST, "유효하지 않은 인증 코드입니다."),
    EXPIRED_EMAIL_VERIFICATION_CODE("7005", HttpStatus.UNAUTHORIZED, "만료된 인증 코드입니다."),
    EMAIL_VERIFICATION_CODE_NOT_FOUND("7006", HttpStatus.NOT_FOUND, "인증 코드를 찾을 수 없습니다."),
    EMAIL_ALREADY_VERIFIED("7007", HttpStatus.BAD_REQUEST, "이미 인증된 이메일입니다."),
    EMAIL_VERIFICATION_LIMIT_EXCEEDED("7008", HttpStatus.TOO_MANY_REQUESTS, "인증 코드 발송 횟수를 초과했습니다."),
    INVALID_PASSWORD_RESET_TOKEN("7009", HttpStatus.BAD_REQUEST, "유효하지 않은 비밀번호 재설정 토큰입니다."),
    EXPIRED_PASSWORD_RESET_TOKEN("7010", HttpStatus.UNAUTHORIZED, "만료된 비밀번호 재설정 토큰입니다."),
    PASSWORD_RESET_TOKEN_NOT_FOUND("7011", HttpStatus.NOT_FOUND, "비밀번호 재설정 토큰을 찾을 수 없습니다."),
    PASSWORD_RESET_TOKEN_ALREADY_USED("7012", HttpStatus.BAD_REQUEST, "이미 사용된 비밀번호 재설정 토큰입니다."),
    PASSWORD_MISMATCH("7013", HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
