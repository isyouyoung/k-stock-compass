package kopo.kstockcompass.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisTokenService {

    @Value("${kis.api.app-key}")
    private String appKey;

    @Value("${kis.api.app-secret}")
    private String appSecret;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private String cachedToken = null;
    private LocalDateTime tokenExpiry = null;

    private static final String REDIS_KEY = "kis:access_token";

    public synchronized String getAccessToken() {
        // 1. 메모리 캐시 먼저 확인
        if (cachedToken != null && tokenExpiry != null
                && LocalDateTime.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }

        // 2. Redis에서 확인 (실패해도 계속 진행)
        try {
            String redisToken = redisTemplate.opsForValue().get(REDIS_KEY);
            if (redisToken != null) {
                cachedToken = redisToken;
                tokenExpiry = LocalDateTime.now().plusHours(23);
                return cachedToken;
            }
        } catch (Exception e) {
            log.warn("Redis 연결 실패, 메모리 캐시 사용: {}", e.getMessage());
        }

        // 3. 새로 발급
        log.info("KIS 액세스 토큰 새로 발급 중...");
        try {
            String response = webClient.post()
                    .uri("https://openapi.koreainvestment.com:9443/oauth2/tokenP")
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of(
                            "grant_type", "client_credentials",
                            "appkey", appKey,
                            "appsecret", appSecret
                    ))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            cachedToken = root.path("access_token").asText();
            tokenExpiry = LocalDateTime.now().plusHours(23);

            // 4. Redis 저장 (실패해도 계속 진행)
            try {
                redisTemplate.opsForValue().set(REDIS_KEY, cachedToken, 23, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Redis 저장 실패, 메모리 캐시만 사용: {}", e.getMessage());
            }

            log.info("KIS 액세스 토큰 발급 완료");
            return cachedToken;

        } catch (Exception e) {
            log.error("KIS 토큰 발급 실패: {}", e.getMessage());
            throw new RuntimeException("KIS 토큰 발급 실패: " + e.getMessage());
        }
    }
}