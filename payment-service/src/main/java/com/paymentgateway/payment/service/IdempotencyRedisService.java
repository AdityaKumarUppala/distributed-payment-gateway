package com.paymentgateway.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.payment.dto.PaymentResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Fast Redis Cache layer for sub-millisecond Idempotency resolution.
 * Pair with MySQL unique constraint for ACID consistency and data durability.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyRedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    public Optional<PaymentResponseDto> getCachedPayment(Long merchantId, String idempotencyKey) {
        String redisKey = buildKey(merchantId, idempotencyKey);
        try {
            String json = redisTemplate.opsForValue().get(redisKey);
            if (json != null && !json.isBlank()) {
                log.info("Redis Idempotency Cache HIT for key: {}", redisKey);
                return Optional.of(objectMapper.readValue(json, PaymentResponseDto.class));
            }
        } catch (Exception e) {
            log.warn("Redis idempotency read failed, falling back to DB: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public void cachePayment(Long merchantId, String idempotencyKey, PaymentResponseDto responseDto) {
        String redisKey = buildKey(merchantId, idempotencyKey);
        try {
            String json = objectMapper.writeValueAsString(responseDto);
            redisTemplate.opsForValue().set(redisKey, json, IDEMPOTENCY_TTL);
            log.info("Cached idempotency record in Redis for key: {}", redisKey);
        } catch (Exception e) {
            log.warn("Failed to cache idempotency key in Redis: {}", e.getMessage());
        }
    }

    private String buildKey(Long merchantId, String idempotencyKey) {
        return "idempotency:merchant:" + merchantId + ":" + idempotencyKey;
    }
}
