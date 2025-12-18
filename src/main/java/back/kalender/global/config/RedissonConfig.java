package back.kalender.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${custom.redis.redisson.address}")
    private String address;

    @Value("${custom.redis.redisson.password}")
    private String redisPassword;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(address)
                .setPassword(redisPassword)
                .setConnectionMinimumIdleSize(10)
                .setConnectionPoolSize(64)
                .setConnectTimeout(3000)
                .setTimeout(3000)
                .setRetryAttempts(2)
                .setRetryInterval(500);

        return Redisson.create(config);
    }
}
