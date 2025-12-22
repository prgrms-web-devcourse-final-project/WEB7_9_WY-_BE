package back.kalender.global.common.constant;


public final class HttpHeaders {
    
    private HttpHeaders() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    
    //  Authorization 헤더 이름
    public static final String AUTHORIZATION = "Authorization";
    
    
    //Bearer 토큰 접두사
    public static final String BEARER_PREFIX = "Bearer ";
    
    /**
     * Bearer 토큰을 포함한 Authorization 헤더 값 생성
     * @param token JWT 토큰
     * @return "Bearer {token}" 형식의 문자열
     */
    public static String createBearerToken(String token) {
        return BEARER_PREFIX + token;
    }
}

