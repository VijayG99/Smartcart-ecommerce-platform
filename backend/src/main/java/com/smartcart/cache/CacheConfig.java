package com.smartcart.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis cache setup.
 *  - Keys are readable strings (e.g. "smartcart::products::5").
 *  - Values are JSON (human-readable in redis-cli, not Java serialization).
 *  - TTL is a safety net: even if an eviction is ever missed, stale data disappears on its own.
 * Spring Boot builds the RedisCacheManager from the RedisCacheConfiguration bean below.
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    @Bean
    RedisCacheConfiguration redisCacheConfiguration(@Value("${smartcart.cache.product-ttl-minutes}") long ttlMinutes) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(ttlMinutes))
                .prefixCacheNameWith("smartcart::")
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(mapper)));
    }

    /**
     * If Redis is down, the cache is skipped and the request is served from PostgreSQL.
     * A cache outage must slow the app down, not break it.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override public void handleCacheGetError(RuntimeException e, Cache c, Object key) {
                log.warn("Cache GET failed for {}::{} - falling back to database: {}", c.getName(), key, e.getMessage());
            }
            @Override public void handleCachePutError(RuntimeException e, Cache c, Object key, Object value) {
                log.warn("Cache PUT failed for {}::{}: {}", c.getName(), key, e.getMessage());
            }
            @Override public void handleCacheEvictError(RuntimeException e, Cache c, Object key) {
                log.error("Cache EVICT failed for {}::{} - entry may be stale until TTL expires: {}",
                        c.getName(), key, e.getMessage());
            }
            @Override public void handleCacheClearError(RuntimeException e, Cache c) {
                log.error("Cache CLEAR failed for {}: {}", c.getName(), e.getMessage());
            }
        };
    }
}
