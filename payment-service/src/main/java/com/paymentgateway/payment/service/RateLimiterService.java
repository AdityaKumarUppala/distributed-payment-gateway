package com.paymentgateway.payment.service;

import com.paymentgateway.payment.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Distributed Rate Limiter backed by Redis.
 * Enforces per-merchant throughput quotas (e.g. 60 requests per minute).
 * Demonstrates resilient fallback: If Redis is unreachable, requests are allowed through with a warning.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${ratelimit.max-requests-per-minute:60}")
    private long maxRequestsPerMinute;

    public void checkRateLimit(Long merchantId) {
        String key = "ratelimit:merchant:" + merchantId;

        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);

            if (currentCount != null && currentCount == 1) {
                // First request in this window -> set 60s TTL
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }

            if (currentCount != null && currentCount > maxRequestsPerMinute) {
                log.warn("Merchant {} exceeded rate limit (current: {}, max: {})",
                        merchantId, currentCount, maxRequestsPerMinute);
                throw new RateLimitExceededException(String.format(
                        "Rate limit exceeded for merchant %d. Max allowed: %d requests/minute. Please retry shortly.",
                        merchantId, maxRequestsPerMinute));
            }
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            // Resilient failure mode: If Redis cluster goes down, allow traffic to prevent system outage
            log.warn("Redis rate limiter unavailable. Falling back gracefully. Error: {}", e.getMessage());
        }
    }
}
