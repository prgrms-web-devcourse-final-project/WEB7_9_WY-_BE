package back.kalender.global.common.constant;

/**
 * Security 관련 상수
 */
public final class SecurityConstants {
    
    private SecurityConstants() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * JwtAuthFilter에서 발생한 예외를 저장하는 request attribute 키
     */
    public static final String AUTH_FILTER_EXCEPTION_ATTR = "AUTH_FILTER_EXCEPTION";
}
