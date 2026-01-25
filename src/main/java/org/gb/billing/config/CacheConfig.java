package org.gb.billing.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for the billing application.
 * Uses Caffeine for high-performance in-memory caching of billing plans and analytics data.
 * 
 * Cache Strategy:
 * - Plans cache: 10-minute TTL, max 1000 entries (read-heavy, infrequent updates)
 * - Analytics cache: 1-minute TTL, max 500 entries (frequently changing data)
 * 
 * @see org.gb.billing.service.PlanService for plan caching usage
 * @see org.gb.billing.service.AnalyticsService for analytics caching usage
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configures Caffeine cache manager with custom settings per cache.
     * 
     * Cache Specifications:
     * - plans: 10-minute expiration, 1000 max entries
     * - analytics: 1-minute expiration, 500 max entries
     * 
     * @return configured CacheManager instance
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("plans", "analytics");
        
        // Plans cache: longer TTL for relatively static data
        cacheManager.registerCustomCache("plans", 
            Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats()
                .build());
        
        // Analytics cache: shorter TTL for frequently changing metrics
        cacheManager.registerCustomCache("analytics", 
            Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats()
                .build());
        
        return cacheManager;
    }
}
