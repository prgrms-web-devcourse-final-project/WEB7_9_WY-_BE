package back.kalender.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Common 0
    UNAUTHORIZED("002",HttpStatus.UNAUTHORIZED,"로그인이 필요합니다."),
    INTERNAL_SERVER_ERROR("003",HttpStatus.INTERNAL_SERVER_ERROR,"서버에서 오류가 발생했습니다."),
    BAD_REQUEST("001", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    // User 1000
    USER_NOT_FOUND("1001", HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    DUPLICATE_NICKNAME("1002", HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

    // Artist 2000
    ARTIST_NOT_FOUND("2001",HttpStatus.NOT_FOUND,"아티스트를 찾을 수 없습니다."),
    ALREADY_FOLLOWED("2002",HttpStatus.CONFLICT,"이미 팔로우한 아티스트입니다."),
    ARTIST_NOT_FOLLOWED("2003", HttpStatus.BAD_REQUEST, "팔로우 상태가 아닙니다."),

    // Party 3000
    // 3001-3099: 파티 기본 CRUD
    PARTY_NOT_FOUND("3001", HttpStatus.NOT_FOUND, "파티를 찾을 수 없습니다."),
    PARTY_ALREADY_CLOSED("3002", HttpStatus.BAD_REQUEST, "이미 마감된 파티입니다."),
    PARTY_ALREADY_COMPLETED("3003", HttpStatus.BAD_REQUEST, "이미 종료된 파티입니다."),
    PARTY_ALREADY_CANCELLED("3004", HttpStatus.BAD_REQUEST, "이미 취소된 파티입니다."),
    PARTY_CREATION_FAILED("3005", HttpStatus.INTERNAL_SERVER_ERROR, "파티 생성에 실패했습니다."),
    PARTY_UPDATE_FAILED("3006", HttpStatus.INTERNAL_SERVER_ERROR, "파티 수정에 실패했습니다."),
    PARTY_DELETE_FAILED("3007", HttpStatus.INTERNAL_SERVER_ERROR, "파티 삭제에 실패했습니다."),

    // 3100-3199: 파티 권한
    UNAUTHORIZED_PARTY_ACCESS("3101", HttpStatus.FORBIDDEN, "파티에 접근할 권한이 없습니다."),
    UNAUTHORIZED_PARTY_LEADER("3102", HttpStatus.FORBIDDEN, "파티장만 이 작업을 수행할 수 있습니다."),
    UNAUTHORIZED_PARTY_MEMBER("3103", HttpStatus.FORBIDDEN, "파티 멤버만 이 작업을 수행할 수 있습니다."),
    CANNOT_MODIFY_PARTY_NOT_LEADER("3104", HttpStatus.FORBIDDEN, "파티장만 파티를 수정할 수 있습니다."),
    CANNOT_DELETE_PARTY_NOT_LEADER("3105", HttpStatus.FORBIDDEN, "파티장만 파티를 삭제할 수 있습니다."),

    // 3200-3299: 파티 신청 관련
    APPLICATION_NOT_FOUND("3201", HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."),
    ALREADY_APPLIED("3202", HttpStatus.BAD_REQUEST, "이미 신청한 파티입니다."),
    ALREADY_MEMBER("3203", HttpStatus.BAD_REQUEST, "이미 참여중인 파티입니다."),
    CANNOT_APPLY_OWN_PARTY("3204", HttpStatus.BAD_REQUEST, "본인이 만든 파티에는 신청할 수 없습니다."),
    PARTY_FULL("3205", HttpStatus.BAD_REQUEST, "파티 인원이 가득 찼습니다."),
    APPLICATION_ALREADY_PROCESSED("3206", HttpStatus.BAD_REQUEST, "이미 처리된 신청입니다."),
    APPLICATION_ALREADY_APPROVED("3207", HttpStatus.BAD_REQUEST, "이미 승인된 신청입니다."),
    APPLICATION_ALREADY_REJECTED("3208", HttpStatus.BAD_REQUEST, "이미 거절된 신청입니다."),
    CANNOT_CANCEL_APPROVED_APPLICATION("3209", HttpStatus.BAD_REQUEST, "승인된 신청은 취소할 수 없습니다."),
    CANNOT_APPLY_CLOSED_PARTY("3210", HttpStatus.BAD_REQUEST, "마감된 파티에는 신청할 수 없습니다."),
    CANNOT_APPLY_COMPLETED_PARTY("3211", HttpStatus.BAD_REQUEST, "종료된 파티에는 신청할 수 없습니다."),

    // 3300-3399: 파티 멤버 관련
    MEMBER_NOT_FOUND("3301", HttpStatus.NOT_FOUND, "파티 멤버를 찾을 수 없습니다."),
    CANNOT_KICK_PARTY_LEADER("3302", HttpStatus.BAD_REQUEST, "파티장은 강퇴할 수 없습니다."),
    CANNOT_LEAVE_AS_PARTY_LEADER("3303", HttpStatus.BAD_REQUEST, "파티장은 파티를 나갈 수 없습니다. 파티를 삭제해주세요."),
    NOT_PARTY_MEMBER("3304", HttpStatus.BAD_REQUEST, "파티 멤버가 아닙니다."),

    // 3400-3499: 파티 유효성 검증
    INVALID_PARTY_TYPE("3401", HttpStatus.BAD_REQUEST, "잘못된 파티 타입입니다."),
    INVALID_MAX_MEMBERS("3402", HttpStatus.BAD_REQUEST, "최대 인원은 2명 이상 10명 이하여야 합니다."),
    INVALID_DEPARTURE_TIME("3403", HttpStatus.BAD_REQUEST, "출발 시간은 현재 시간 이후여야 합니다."),
    INVALID_PARTY_NAME("3404", HttpStatus.BAD_REQUEST, "파티 이름은 2자 이상 50자 이하여야 합니다."),
    INVALID_DESCRIPTION("3405", HttpStatus.BAD_REQUEST, "설명은 500자 이하여야 합니다."),
    INVALID_LOCATION("3406", HttpStatus.BAD_REQUEST, "위치는 100자 이하여야 합니다."),
    DEPARTURE_LOCATION_REQUIRED("3407", HttpStatus.BAD_REQUEST, "출발 위치는 필수입니다."),
    ARRIVAL_LOCATION_REQUIRED("3408", HttpStatus.BAD_REQUEST, "도착 위치는 필수입니다."),
    DEPARTURE_TIME_REQUIRED("3409", HttpStatus.BAD_REQUEST, "출발 시간은 필수입니다."),
    INVALID_PREFERRED_GENDER("3410", HttpStatus.BAD_REQUEST, "잘못된 선호 성별입니다."),
    INVALID_PREFERRED_AGE("3411", HttpStatus.BAD_REQUEST, "잘못된 선호 연령대입니다."),
    INVALID_TRANSPORT_TYPE("3412", HttpStatus.BAD_REQUEST, "잘못된 이동 수단입니다."),
    CANNOT_REDUCE_MAX_MEMBERS("3413", HttpStatus.BAD_REQUEST, "현재 인원보다 적게 최대 인원을 설정할 수 없습니다."),

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
