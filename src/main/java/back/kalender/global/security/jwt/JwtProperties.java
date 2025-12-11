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
        private final long access;
        private final long refresh;

        public TokenExpiration(long access, long refresh) {
            this.access = access;
            this.refresh = refresh;
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
