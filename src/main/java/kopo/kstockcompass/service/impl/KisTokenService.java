package kopo.kstockcompass.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Map;

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

    // 토큰 캐싱 (매번 발급하면 낭비)
    private String cachedToken = null;
    private LocalDateTime tokenExpiry = null;

    public synchronized String getAccessToken() {
        // 캐싱된 토큰이 있고 만료 전이면 그대로 반환
        if (cachedToken != null && tokenExpiry != null
                && LocalDateTime.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }

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
            // 토큰 유효기간 23시간으로 설정 (실제는 24시간)
            tokenExpiry = LocalDateTime.now().plusHours(23);

            log.info("KIS 액세스 토큰 발급 완료");
            return cachedToken;

        } catch (Exception e) {
            log.error("KIS 토큰 발급 실패: {}", e.getMessage());
            throw new RuntimeException("KIS 토큰 발급 실패: " + e.getMessage());
        }
    }
}