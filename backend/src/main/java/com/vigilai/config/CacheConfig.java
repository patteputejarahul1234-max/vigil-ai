package com.vigilai.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Cache type is driven by app.cache-type (default "simple", an in-memory
 * ConcurrentHashMap — zero setup for local dev). Set CACHE_TYPE=redis
 * (as docker-compose does) to switch to real Redis with the TTLs below,
 * without touching a single @Cacheable annotation elsewhere in the code.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Only wired up when spring.cache.type=redis; harmless no-op otherwise. */
    @org.springframework.context.annotation.Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));

        return builder -> builder
                .withCacheConfiguration("workspaces", defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("workspace-members", defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("analytics", defaultConfig.entryTtl(Duration.ofMinutes(2)));
    }
}
