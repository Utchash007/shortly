package com.shortly.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis wiring for the cache-aside layer.
 *
 * <p>Connection settings (host, port, password, timeout) come from
 * {@code spring.data.redis.*} in {@code application.yml} and are applied by
 * Boot's Lettuce auto-configuration. This class pins String serialization for
 * keys and values so cached entries stay human-readable JSON in
 * {@code redis-cli}.
 */
@Configuration
public class RedisConfig {

    /**
     * Creates the string template used for all cache operations.
     *
     * @param connectionFactory Lettuce-backed factory from Boot auto-configuration
     * @return template with String serializers on keys, values and hashes
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}
