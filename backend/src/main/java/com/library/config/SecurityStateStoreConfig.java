package com.library.config;

import com.library.security.InMemorySecurityStateStore;
import com.library.security.RedisSecurityStateStore;
import com.library.security.SecurityStateStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;

@Configuration
public class SecurityStateStoreConfig {

    @Bean
    @ConditionalOnProperty(name = "anti-crawler.store.type", havingValue = "redis")
    public SecurityStateStore redisSecurityStateStore(StringRedisTemplate redisTemplate) {
        return new RedisSecurityStateStore(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(SecurityStateStore.class)
    public SecurityStateStore inMemorySecurityStateStore(Clock clock) {
        return new InMemorySecurityStateStore(clock);
    }
}
