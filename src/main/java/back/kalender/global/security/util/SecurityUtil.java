package back.kalender.global.security.util;

import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import back.kalender.global.security.user.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    /**
     * SecurityContext에서 현재 인증된 사용자의 ID를 가져옵니다.
     * @return 사용자 ID, 인증되지 않은 경우 null
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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

    /**
     * SecurityContext에서 현재 인증된 사용자의 이메일을 가져옵니다.
     * @return 사용자 이메일, 인증되지 않은 경우 null
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * SecurityContext에서 현재 인증된 사용자의 CustomUserDetails를 가져옵니다.
     * @return CustomUserDetails, 인증되지 않은 경우 null
     */
    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomUserDetails) {
                return (CustomUserDetails) principal;
            }
        }
        return null;
    }
}

