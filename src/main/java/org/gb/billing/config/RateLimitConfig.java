package org.gb.billing.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.caffeine.CaffeineProxyManager;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.distributed.remote.RemoteBucketState;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for rate limiting using Bucket4j with Caffeine backend.
 * 
 * Provides in-memory rate limit bucket storage with automatic expiration.
 * Buckets are keyed by tenantId and expire after 10 minutes of inactivity.
 * 
 * @see org.gb.billing.security.RateLimitingFilter for rate limiting enforcement
 */
@Configuration
public class RateLimitConfig {
    
    /**
     * Creates a ProxyManager for managing rate limit buckets.
     * 
     * Configuration:
     * - Expiration: 10 minutes after last access (removes inactive users)
     * - Maximum size: 10,000 buckets (supports large user base)
     * - Backend: Caffeine in-memory cache (fast, no external dependencies)
     * 
     * @return ProxyManager instance for rate limit bucket management
     */
    @Bean
    public ProxyManager<String> rateLimitBuckets() {
        Caffeine<String, RemoteBucketState> builder = (Caffeine) Caffeine.newBuilder()
                .maximumSize(10000);
        return new CaffeineProxyManager<>(builder, java.time.Duration.ofMinutes(10));
    }
}
