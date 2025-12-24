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
    FORBIDDEN("005", HttpStatus.FORBIDDEN, "권한이 없습니다."),

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
    ALREADY_JOINED_BEFORE("3213",  HttpStatus.BAD_REQUEST, "한번 나간 파티는 다시 들어갈 수 없습니다."),

    // 3400-3499: 파티 유효성 검증
    CANNOT_REDUCE_MAX_MEMBERS("3413", HttpStatus.BAD_REQUEST, "현재 인원보다 적게 최대 인원을 설정할 수 없습니다."),

    // Schedule 4000
    SCHEDULE_NOT_FOUND("4001", HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."),

    // Performance 5000
    PERFORMANCE_NOT_FOUND("5001", HttpStatus.NOT_FOUND, "공연을 찾을 수 없습니다."),
    PERFORMANCE_HALL_NOT_FOUND("5002", HttpStatus.NOT_FOUND, "공연장을 찾을 수 없습니다."),
    PRICE_GRADE_NOT_FOUND("5003", HttpStatus.NOT_FOUND, "가격 등급을 찾을 수 없습니다."),
    PERFORMANCE_SEAT_NOT_FOUND("5004", HttpStatus.NOT_FOUND, "공연 좌석을 찾을 수 없습니다."),

    // Reservation 6000
    RESERVATION_NOT_FOUND("6001", HttpStatus.NOT_FOUND, "예매를 찾을 수 없습니다."),
    RESERVATION_EXPIRED("6002", HttpStatus.BAD_REQUEST, "만료된 예매입니다."),
    ALREADY_PAID_RESERVATION("6003", HttpStatus.BAD_REQUEST, "이미 결제된 예매입니다."),
    PARTIAL_RELEASE_NOT_ALLOWED("6004", HttpStatus.BAD_REQUEST, "부분 좌석 해제는 허용되지 않습니다."),
    SEAT_STATE_INCONSISTENT("6005", HttpStatus.CONFLICT, "좌석 상태가 일치하지 않습니다."),
    SCHEDULE_NOT_AVAILABLE("6006", HttpStatus.BAD_REQUEST, "예매가 불가능한 회차입니다."),
    INVALID_RESERVATION_STATUS("6007", HttpStatus.BAD_REQUEST, "유효하지 않은 예매 상태입니다."),
    CANCEL_DEADLINE_PASSED("6008", HttpStatus.BAD_REQUEST, "취소 가능 기한이 지났습니다."),
    NO_SEATS_RESERVED("6009", HttpStatus.BAD_REQUEST, "예매된 좌석이 없습니다."),

    // 6100-6199: 대기열 토큰 관련
    WAITING_TOKEN_REQUIRED("6101", HttpStatus.BAD_REQUEST, "대기열 토큰이 필요합니다."),
    INVALID_WAITING_TOKEN("6102", HttpStatus.BAD_REQUEST, "유효하지 않은 대기열 토큰입니다."),
    WAITING_TOKEN_MISMATCH("6103", HttpStatus.BAD_REQUEST, "대기열 토큰이 일치하지 않습니다."),
    DEVICE_ID_MISMATCH("6104", HttpStatus.BAD_REQUEST, "디바이스 ID가 일치하지 않습니다."),
    SCHEDULE_MISMATCH("6105", HttpStatus.BAD_REQUEST, "회차가 일치하지 않습니다."),
    QSID_EXPIRED("6106", HttpStatus.BAD_REQUEST, "만료된 QSID입니다."),
    DEVICE_ALREADY_USED("6107", HttpStatus.CONFLICT, "이미 다른 기기로 접속중인 세션이 있습니다."),
    NOT_IN_ACTIVE("6108", HttpStatus.BAD_REQUEST, "ACTIVE 상태가 아닙니다."),

    // 6200-6299: 예매 세션 관련
    BOOKING_SESSION_EXPIRED("6201", HttpStatus.UNAUTHORIZED, "예매 세션이 만료되었습니다."),
    INVALID_BOOKING_SESSION("6202", HttpStatus.BAD_REQUEST, "유효하지 않은 예매 세션입니다."),
    RESERVATION_ALREADY_EXISTS("6203", HttpStatus.CONFLICT, "진행 중인 예매가 이미 존재합니다."),

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
    PASSWORD_MISMATCH("7013", HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),

    // 7014-7019: 이메일 발송 관련
    EMAIL_CONFIGURATION_ERROR("7014", HttpStatus.INTERNAL_SERVER_ERROR, "이메일 서버 설정이 올바르지 않습니다."),
    INVALID_EMAIL_ADDRESS("7015", HttpStatus.BAD_REQUEST, "유효하지 않은 이메일 주소입니다."),
    EMAIL_TEMPLATE_ERROR("7016", HttpStatus.INTERNAL_SERVER_ERROR, "이메일 템플릿 처리 중 오류가 발생했습니다."),
    EMAIL_SEND_FAILED("7017", HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다."),

    // 7018-7023: JWT 토큰 검증 관련
    JWT_TOKEN_NULL_OR_EMPTY("7018", HttpStatus.UNAUTHORIZED, "JWT 토큰이 없거나 비어있습니다."),
    JWT_TOKEN_EXPIRED("7019", HttpStatus.UNAUTHORIZED, "JWT 토큰이 만료되었습니다."),
    JWT_TOKEN_MALFORMED("7020", HttpStatus.UNAUTHORIZED, "JWT 토큰 형식이 올바르지 않습니다."),
    JWT_TOKEN_SIGNATURE_INVALID("7021", HttpStatus.UNAUTHORIZED, "JWT 토큰 서명이 유효하지 않습니다."),
    JWT_TOKEN_UNSUPPORTED("7022", HttpStatus.UNAUTHORIZED, "지원하지 않는 JWT 토큰입니다."),
    JWT_TOKEN_ILLEGAL_ARGUMENT("7023", HttpStatus.UNAUTHORIZED, "JWT 토큰 파싱 중 오류가 발생했습니다."),

    // Chat 8000
    // 8001-8099: 채팅방 관련
    CHAT_ROOM_NOT_FOUND("8001", HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_ALREADY_EXISTS("8002", HttpStatus.CONFLICT, "이미 채팅방이 존재합니다."),
    CHAT_ROOM_NOT_ACTIVE("8003", HttpStatus.BAD_REQUEST, "비활성화된 채팅방입니다."),
    UNAUTHORIZED_CHAT_ACCESS("8004", HttpStatus.FORBIDDEN, "채팅방 접근 권한이 없습니다."),

    // 8100-8199: 메시지 관련
    MESSAGE_EMPTY("8101", HttpStatus.BAD_REQUEST, "메시지 내용이 비어있습니다."),
    MESSAGE_TOO_LONG("8102", HttpStatus.BAD_REQUEST, "메시지가 너무 깁니다. (최대 500자)"),

    // 8200-8299: 권한 관련
    LEADER_CANNOT_LEAVE("8201", HttpStatus.BAD_REQUEST, "파티장은 채팅방을 나갈 수 없습니다."),
    CANNOT_KICK_YOURSELF("8202", HttpStatus.BAD_REQUEST, "자기 자신을 강퇴할 수 없습니다."),
    ONLY_LEADER_CAN_KICK("8203", HttpStatus.FORBIDDEN, "파티장만 멤버를 강퇴할 수 있습니다."),
    USER_NOT_IN_PARTY("8204", HttpStatus.NOT_FOUND, "해당 사용자는 파티 멤버가 아닙니다."),

    // Payment 9000
    PAYMENT_NOT_FOUND("9001", HttpStatus.NOT_FOUND, "결제를 찾을 수 없습니다."),
    PAYMENT_AMOUNT_MISMATCH("9002", HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    PAYMENT_ORDER_ID_MISMATCH("9003", HttpStatus.BAD_REQUEST, "주문 ID가 일치하지 않습니다."),
    PAYMENT_CANNOT_CANCEL("9004", HttpStatus.BAD_REQUEST, "취소할 수 없는 결제 상태입니다."),
    PAYMENT_ALREADY_CANCELED("9005", HttpStatus.BAD_REQUEST, "이미 취소된 결제입니다."),
    PAYMENT_GATEWAY_ERROR("9006", HttpStatus.BAD_GATEWAY, "결제 게이트웨이 오류가 발생했습니다."),
    PAYMENT_CANNOT_CONFIRM("9007", HttpStatus.BAD_REQUEST, "승인할 수 없는 결제 상태입니다."),
    PAYMENT_GATEWAY_TIMEOUT("9010", HttpStatus.REQUEST_TIMEOUT, "결제 게이트웨이 타임아웃이 발생했습니다."),
    PAYMENT_IDEMPOTENCY_KEY_REQUIRED("9008", HttpStatus.BAD_REQUEST, "Idempotency-Key 헤더가 필요합니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
