package back.kalender.global.security.util;

import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import back.kalender.global.security.user.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class SecurityUtil {

    // JwtAuthFilter에서 설정한 request attribute 키
    private static final String AUTH_FILTER_EXCEPTION_ATTR = "AUTH_FILTER_EXCEPTION";

    public static Long getCurrentUserId() {
        Authentication authentication = getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomUserDetails) {
                return ((CustomUserDetails) principal).getUserId();
            }
        }
        return null;
    }

    /**
     * SecurityContext에서 현재 인증된 사용자의 ID를 가져옵니다.
     * 인증되지 않은 경우 예외를 발생시킵니다.
     * 
     * JwtAuthFilter에서 발생한 비즈니스 예외(예: USER_NOT_FOUND)가 있으면
     * 해당 예외를 재발생시켜 실제 에러가 401로 덮이지 않도록 합니다.
     * 
     * @return 사용자 ID
     * @throws ServiceException 인증되지 않은 경우 또는 JwtAuthFilter에서 발생한 비즈니스 예외
     */
    public static Long getCurrentUserIdOrThrow() {
        // JwtAuthFilter에서 발생한 비즈니스 예외 확인
        ServiceException authFilterException = getAuthFilterException();
        if (authFilterException != null) {
            // JWT 토큰은 유효하지만 사용자 조회 실패 등의 비즈니스 예외
            // 실제 에러를 재발생시켜 GlobalExceptionHandler가 처리하도록 함
            throw authFilterException;
        }
        
        Long userId = getCurrentUserId();
        if (userId == null) {
            // 인증되지 않은 경우 (토큰이 없거나 유효하지 않음)
            throw new ServiceException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
    
    /**
     * JwtAuthFilter에서 request attribute에 저장한 예외를 가져옵니다.
     * @return ServiceException 또는 null
     */
    private static ServiceException getAuthFilterException() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                Object exception = request.getAttribute(AUTH_FILTER_EXCEPTION_ATTR);
                if (exception instanceof ServiceException) {
                    return (ServiceException) exception;
                }
            }
        } catch (Exception e) {
            // RequestContextHolder가 없거나 예외 발생 시 무시
        }
        return null;
    }


    public static String getCurrentUserEmail() {
        Authentication authentication = getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }


    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomUserDetails) {
                return (CustomUserDetails) principal;
            }
        }
        return null;
    }

    private static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}

