package back.kalender.global.security.util;

import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import back.kalender.global.security.user.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {


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
     * @return 사용자 ID
     * @throws ServiceException 인증되지 않은 경우
     */
    public static Long getCurrentUserIdOrThrow() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
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

