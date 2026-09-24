package com.rpe.cardprocessor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpe.cardprocessor.client.CachedCatalogGateway;
import com.rpe.cardprocessor.service.CatalogProduct;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

/**
 * Cached catalog products are stored as JSON (readable in redis-cli) instead of JDK serialization.
 * Key prefix and TTL come from spring.cache.redis.* in application.yml.
 */
@Configuration
public class CacheConfig {

    @Bean
    RedisCacheManagerBuilderCustomizer catalogProductsCacheCustomizer(ObjectMapper objectMapper) {
        var serializer = new Jackson2JsonRedisSerializer<>(objectMapper, CatalogProduct.class);
        return builder -> builder.withCacheConfiguration(CachedCatalogGateway.CACHE,
                builder.cacheDefaults().serializeValuesWith(SerializationPair.fromSerializer(serializer)));
    }
}
