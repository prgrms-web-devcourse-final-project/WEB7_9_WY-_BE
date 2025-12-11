package back.kalender.global.security.jwt;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "custom.jwt")
public class JwtProperties {

    private final String secret;
    private final TokenExpiration tokenExpiration;

    public JwtProperties(String secret, TokenExpiration tokenExpiration) {
        this.secret = secret;
        this.tokenExpiration = tokenExpiration;
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
}
