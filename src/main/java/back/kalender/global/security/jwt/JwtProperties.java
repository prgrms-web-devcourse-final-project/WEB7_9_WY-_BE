package back.kalender.global.security.jwt;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "custom.jwt")
public class JwtProperties {

    private final String secret;
    private final TokenExpiration tokenExpiration;
    private final CookieProperties cookie;

    public JwtProperties(String secret, TokenExpiration tokenExpiration, CookieProperties cookie) {
        this.secret = secret;
        this.tokenExpiration = tokenExpiration;
        this.cookie = cookie;
    }

    @Getter
    public static class TokenExpiration {
        private static final long MILLIS_PER_SECOND = 1000L;
        private static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
        private static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;
        private static final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;
        private static final long SECONDS_PER_DAY = 24 * 60 * 60L;

        private final long access; // 초 단위
        private final long refresh; // 일 단위

        public TokenExpiration(long access, long refresh) {
            this.access = access;
            this.refresh = refresh;
        }

        public long getAccessInMillis() {
            return access * MILLIS_PER_SECOND;
        }

        public long getRefreshInMillis() {
            return refresh * MILLIS_PER_DAY;
        }

        public long getRefreshInSeconds() {
            return refresh * SECONDS_PER_DAY;
        }
    }

    @Getter
    public static class CookieProperties {
        private final boolean secure;

        public CookieProperties(boolean secure) {
            this.secure = secure;
        }
    }
}
