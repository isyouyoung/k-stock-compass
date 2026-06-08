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

/**
 * [KIS 인증 토큰 관리 서비스]
 *
 * 설명:
 * 한국투자증권(KIS) Open API 호출에 필요한
 * Access Token을 발급하고 관리하는 서비스입니다.
 *
 * 특징:
 * - 메모리 캐시(JVM RAM) 사용
 * - Redis 캐시 사용
 * - 토큰 만료 시간 관리
 * - Redis 장애 발생 시에도 메모리 캐시 기반 동작 가능
 * - synchronized 키워드를 통한 동시성 제어
 *
 * 전체 흐름:
 * 1. JVM 메모리 캐시 확인
 * 2. Redis 캐시 확인
 * 3. 둘 다 없으면 KIS 서버에서 새 토큰 발급
 * 4. 메모리 + Redis 모두 저장
 */
@Slf4j
// 로그 출력을 위한 Lombok 어노테이션 (log.info, log.warn 등 사용 가능)
@Service
// 비즈니스 로직을 담당하는 서비스 계층임을 스프링에 알림
@RequiredArgsConstructor
// final 필드들을 자동으로 생성자 주입 (IoC/DI 원칙)
public class KisTokenService {

    /**
     * KIS Open API App Key
     */
    @Value("${kis.api.app-key}")
    private String appKey;

    /**
     * KIS Open API App Secret
     */
    @Value("${kis.api.app-secret}")
    private String appSecret;

    /**
     * 외부 API 통신용 WebClient
     */
    private final WebClient webClient;

    /**
     * JSON 응답 파싱용 ObjectMapper
     */
    private final ObjectMapper objectMapper;

    /**
     * Redis 저장/조회용 Template
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * JVM 메모리 캐시 영역
     *
     * 특징:
     * - 현재 서버 프로세스 내부 RAM에 저장됨
     * - 속도가 가장 빠름
     * - 서버 재시작 시 초기화됨
     */
    private String cachedToken = null;

    /**
     * 메모리 캐시 토큰 만료 시간
     */
    private LocalDateTime tokenExpiry = null;

    /**
     * Redis 저장 Key
     *
     * Redis 내부 저장 형태:
     * kis:access_token → eyJhbGciOi...
     */
    private static final String REDIS_KEY = "kis:access_token";

    /**
     * [KIS Access Token 조회]
     *
     * 역할:
     * KIS API 호출에 필요한 Access Token을 반환합니다.
     *
     * 동작 흐름:
     *
     * 1. JVM 메모리 캐시 확인
     * 2. Redis 캐시 확인
     * 3. 없으면 KIS 서버에서 신규 발급
     * 4. 메모리 + Redis 저장
     *
     * synchronized:
     * 동시에 여러 스레드가 토큰 발급 API를 중복 호출하는 상황을 방지
     */
    public synchronized String getAccessToken() {

        /**
         * =====================================================
         * 1단계 - JVM 메모리 캐시 확인
         * =====================================================
         *
         * 가장 빠른 조회 방식
         *
         * 현재 Spring 서버 프로세스 내부 RAM에 저장된 토큰 확인
         *
         * 조건:
         * - cachedToken 존재
         * - 만료시간 존재
         * - 현재 시간이 만료 전
         */
        if (cachedToken != null
                && tokenExpiry != null
                && LocalDateTime.now().isBefore(tokenExpiry)) {

            log.info("✅ 메모리 캐시에서 KIS 토큰 반환");

            return cachedToken;
        }

        /**
         * =====================================================
         * 2단계 - Redis 캐시 확인
         * =====================================================
         *
         * 메모리 캐시에 없으면 Redis 조회
         *
         * Redis는 별도 프로세스로 동작하는 인메모리 DB
         * → 서버 재시작 후에도 토큰 유지 가능
         */
        try {

            String redisToken =
                    redisTemplate.opsForValue().get(REDIS_KEY);

            // Redis에 토큰 존재 시
            if (redisToken != null) {

                log.info("✅ Redis 캐시에서 KIS 토큰 반환");

                /**
                 * Redis에서 꺼낸 토큰을
                 * 다시 JVM 메모리 캐시에 적재
                 *
                 * 이유:
                 * 다음 요청부터 Redis 접근 없이
                 * 메모리에서 초고속 반환 가능
                 */
                cachedToken = redisToken;

                /**
                 * KIS 토큰 유효시간:
                 * 약 24시간
                 *
                 * 안전하게 23시간으로 설정
                 */
                tokenExpiry = LocalDateTime.now().plusHours(23);

                return cachedToken;
            }

        } catch (Exception e) {

            /**
             * Redis 장애 발생 시에도
             * 시스템 전체가 죽지 않도록 예외를 삼킴
             *
             * → 장애 허용(Fail Soft) 구조
             */
            log.warn("Redis 연결 실패, 메모리 캐시 사용: {}", e.getMessage());
        }

        /**
         * =====================================================
         * 3단계 - KIS 서버에서 신규 토큰 발급
         * =====================================================
         *
         * 메모리 + Redis 둘 다 없을 경우
         * 실제 한국투자증권 인증 서버 호출
         */
        log.info("🔄 KIS 액세스 토큰 새로 발급 중...");

        try {

            String response = webClient.post()

                    .uri("https://openapi.koreainvestment.com:9443/oauth2/tokenP")

                    .header("Content-Type", "application/json")

                    /**
                     * OAuth2 Client Credentials 방식
                     */
                    .bodyValue(Map.of(
                            "grant_type", "client_credentials",
                            "appkey", appKey,
                            "appsecret", appSecret
                    ))

                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // JSON 응답 파싱
            JsonNode root = objectMapper.readTree(response);

            /**
             * 발급된 Access Token 추출
             */
            cachedToken = root.path("access_token").asText();

            /**
             * 메모리 캐시 만료 시간 설정
             */
            tokenExpiry = LocalDateTime.now().plusHours(23);

            /**
             * =====================================================
             * 4단계 - Redis 저장
             * =====================================================
             *
             * 서버 재시작 이후에도 토큰 유지 가능하도록
             * Redis에 저장
             */
            try {

                redisTemplate.opsForValue().set(
                        REDIS_KEY,
                        cachedToken,
                        23,
                        TimeUnit.HOURS
                );

                log.info("💾 Redis 토큰 저장 완료");

            } catch (Exception e) {

                /**
                 * Redis 저장 실패해도
                 * 메모리 캐시만으로 계속 서비스 가능
                 */
                log.warn("Redis 저장 실패, 메모리 캐시만 사용: {}", e.getMessage());
            }

            log.info("✅ KIS 액세스 토큰 발급 완료");

            return cachedToken;

        } catch (Exception e) {

            log.error("❌ KIS 토큰 발급 실패: {}", e.getMessage());

            throw new RuntimeException(
                    "KIS 토큰 발급 실패: " + e.getMessage()
            );
        }
    }
}