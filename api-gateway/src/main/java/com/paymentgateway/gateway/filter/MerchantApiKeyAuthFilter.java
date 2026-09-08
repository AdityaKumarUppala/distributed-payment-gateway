package com.paymentgateway.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.gateway.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Security Filter for Gateway-level Authentication.
 * Intercepts transaction endpoints (/api/v1/payments/**, /api/v1/refunds/**)
 * and enforces valid X-API-Key verification.
 */
@Component
@Slf4j
public class MerchantApiKeyAuthFilter implements GlobalFilter, Ordered {

    public static final String API_KEY_HEADER = "X-API-Key";

    private static final List<String> PROTECTED_PREFIXES = List.of(
            "/api/v1/payments",
            "/api/v1/refunds"
    );

    @Value("${services.merchant-service.url:http://localhost:8082}")
    private String merchantServiceUrl;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    public MerchantApiKeyAuthFilter(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        boolean isProtected = PROTECTED_PREFIXES.stream().anyMatch(path::startsWith);
        if (!isProtected) {
            return chain.filter(exchange);
        }

        String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Unauthorized request to protected endpoint {}: Missing {}", path, API_KEY_HEADER);
            return writeUnauthorizedResponse(exchange, "Missing required '" + API_KEY_HEADER + "' authentication header");
        }

        log.debug("Authenticating X-API-Key for path: {}", path);

        return webClientBuilder.build()
                .get()
                .uri(merchantServiceUrl + "/api/v1/merchants/validate-key")
                .header(API_KEY_HEADER, apiKey)
                .retrieve()
                .bodyToMono(com.paymentgateway.gateway.dto.MerchantValidationResponseDto.class)
                .flatMap(validation -> {
                    if (validation != null && validation.isValid() && validation.getMerchantId() != null) {
                        log.debug("API Key validated for merchant ID: {}", validation.getMerchantId());

                        // Inject verified merchant ID into downstream headers
                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("X-Authenticated-Merchant-Id", String.valueOf(validation.getMerchantId()))
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    } else {
                        log.warn("Invalid or inactive API Key provided for path: {}", path);
                        return writeUnauthorizedResponse(exchange, "Invalid, expired, or inactive API Key");
                    }
                })
                .onErrorResume(ex -> {
                    log.error("Merchant validation service error during authentication: ", ex);
                    return writeUnauthorizedResponse(exchange, "Authentication service temporarily unavailable or invalid key");
                });
    }

    private Mono<Void> writeUnauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("UNAUTHORIZED")
                .message(message)
                .path(exchange.getRequest().getURI().getPath())
                .build();

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(errorResponse).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = "{\"error\":\"UNAUTHORIZED\"}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
