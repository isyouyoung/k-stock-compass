package kopo.kstockcompass.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.kstockcompass.dto.MarketIndexDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import kopo.kstockcompass.dto.StockItemDTO;

/**
 * [KIS 실시간 주가 서비스]
 * 설명:
 * 한국투자증권(KIS) Open API를 연동하여
 * 실시간 현재가, 종목 상세 정보, 코스피/코스닥 지수 정보를 조회하는 서비스 계층입니다.
 *
 * 특징:
 * - WebClient 기반 비동기 HTTP 통신 사용
 * - KIS Access Token 인증 방식 적용
 * - 실시간 시세 데이터 처리
 * - JSON 응답 파싱 후 DTO 변환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KisStockService {

    /**
     * KIS Open API App Key
     * application.properties(or yml)에 저장된 값을 주입받음
     */
    @Value("${kis.api.app-key}")
    private String appKey;

    /**
     * KIS Open API App Secret
     */
    @Value("${kis.api.app-secret}")
    private String appSecret;

    /**
     * 외부 API HTTP 통신용 WebClient
     */
    private final WebClient webClient;

    /**
     * JSON 파싱용 ObjectMapper
     */
    private final ObjectMapper objectMapper;

    /**
     * KIS Access Token 발급/관리 서비스
     * Redis 기반으로 토큰 캐싱 처리
     */
    private final KisTokenService kisTokenService;

    /**
     * [실시간 현재가 조회]
     *
     * 역할:
     * 특정 종목코드(예: 삼성전자 005930)의
     * 현재 실시간 체결 가격을 조회합니다.
     *
     * 흐름:
     * 1. Redis에서 KIS Access Token 조회
     * 2. KIS 현재가 API 호출
     * 3. 응답 코드(rt_cd) 검증
     * 4. 현재가(stck_prpr) 추출
     * 5. Long 타입으로 반환
     */
    public long getCurrentPrice(String stockCode) {

        try {

            // Redis 캐시에 저장된 KIS Access Token 조회
            String token = kisTokenService.getAccessToken();

            // KIS 현재가 조회 API 호출
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("openapi.koreainvestment.com")
                            .port(9443)
                            .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", stockCode)
                            .build())

                    // HTTP Header 설정
                    .header("Content-Type", "application/json")
                    .header("authorization", "Bearer " + token)
                    .header("appkey", appKey)
                    .header("appsecret", appSecret)

                    /**
                     * tr_id:
                     * KIS API에서 어떤 API를 호출하는지 식별하는 거래 ID
                     */
                    .header("tr_id", "FHKST01010100")

                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // JSON 문자열 파싱
            JsonNode root = objectMapper.readTree(response);

            /**
             * rt_cd:
             * KIS API 비즈니스 처리 결과 코드
             *
             * 0 = 정상
             * 그 외 = 실패
             */
            String rtCd = root.path("rt_cd").asText();

            // 비즈니스 오류 처리
            if (!"0".equals(rtCd)) {

                String msg = root.path("msg1").asText();

                log.error("KIS API 비즈니스 로직 오류: {} (코드: {})", msg, rtCd);

                throw new RuntimeException("KIS API 오류: " + msg);
            }

            // 실시간 현재가 추출
            String price = root.path("output")
                    .path("stck_prpr")
                    .asText();

            log.info("KIS 현재가 조회 성공: {} → {}원", stockCode, price);

            return Long.parseLong(price);

        } catch (Exception e) {

            log.error("KIS 현재가 조회 실패: {} - {}", stockCode, e.getMessage());

            throw new RuntimeException("현재가 조회 실패: " + e.getMessage());
        }
    }

    /**
     * [실시간 종목 상세 조회]
     *
     * 역할:
     * 특정 종목의 상세 시세 정보를 조회합니다.
     *
     * 조회 데이터:
     * - 현재가
     * - 등락률
     * - 전일대비
     * - 시가
     * - 고가
     * - 저가
     * - 거래량
     * - 시가총액
     * - 52주 최고가
     *
     * 특징:
     * React 상세 페이지 렌더링용 DTO 생성
     */
    public StockItemDTO getStockDetail(String stockCode) {

        try {

            // Redis 기반 Access Token 조회
            String token = kisTokenService.getAccessToken();

            // KIS 상세 시세 API 호출
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("openapi.koreainvestment.com")
                            .port(9443)
                            .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", stockCode)
                            .build())

                    .header("Content-Type", "application/json")
                    .header("authorization", "Bearer " + token)
                    .header("appkey", appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id", "FHKST01010100")

                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);

            // KIS 응답 성공 여부 검증
            String rtCd = root.path("rt_cd").asText();

            if (!"0".equals(rtCd)) {

                String msg = root.path("msg1").asText();

                log.error("KIS API 오류: {} (코드: {})", msg, rtCd);

                throw new RuntimeException("KIS API 오류: " + msg);
            }

            JsonNode output = root.path("output");

            String price = output.path("stck_prpr").asText(); // 현재가
            String vs = output.path("prdy_vrss").asText(); // 전일대비
            String fltRt = output.path("prdy_ctrt").asText(); // 등락률

            log.info("KIS 상세 조회 성공: {} → {}원 ({}%)", stockCode, price, fltRt);

            /**
             * StockItemDTO 생성
             * React 프론트로 전달될 실시간 시세 데이터 구조
             */
            return new StockItemDTO(

                    stockCode,

                    null, // 종목명 (별도 조회 가능)

                    output.path("stck_prpr").asText(), // 현재가

                    output.path("prdy_ctrt").asText(), // 등락률

                    output.path("prdy_vrss").asText(), // 전일대비

                    null, // 시장구분

                    output.path("stck_oprc").asText(), // 시가

                    output.path("stck_hgpr").asText(), // 고가

                    output.path("stck_lwpr").asText(), // 저가

                    output.path("acml_vol").asText(), // 거래량

                    output.path("hts_avls").asText(), // 시가총액

                    output.path("w52_hgpr").asText() // 52주 최고가
            );

        } catch (Exception e) {

            log.error("KIS 상세 조회 실패: {} - {}", stockCode, e.getMessage());

            throw new RuntimeException("상세 조회 실패: " + e.getMessage());
        }
    }

    /**
     * [시장 지수 조회]
     *
     * 역할:
     * 코스피/코스닥 지수 정보를 조회합니다.
     *
     * 사용 예시:
     * - 0001 → 코스피
     * - 1001 → 코스닥
     *
     * 조회 데이터:
     * - 현재 지수
     * - 등락률
     * - 전일 대비
     * - 시가
     * - 고가
     * - 저가
     */
    public MarketIndexDTO getIndexPrice(String indexCode) {

        try {

            // Redis 기반 Access Token 조회
            String token = kisTokenService.getAccessToken();

            // KIS 지수 조회 API 호출
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("openapi.koreainvestment.com")
                            .port(9443)
                            .path("/uapi/domestic-stock/v1/quotations/inquire-index-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                            .queryParam("FID_INPUT_ISCD", indexCode)
                            .build())

                    .header("Content-Type", "application/json")
                    .header("authorization", "Bearer " + token)
                    .header("appkey", appKey)
                    .header("appsecret", appSecret)

                    /**
                     * 업종/지수 조회용 TR ID
                     */
                    .header("tr_id", "FHPUP02100000")

                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);

            String rtCd = root.path("rt_cd").asText();

            // API 비즈니스 오류 처리
            if (!"0".equals(rtCd)) {

                throw new RuntimeException(
                        "KIS 지수 API 오류: " + root.path("msg1").asText()
                );
            }

            JsonNode output = root.path("output");

            log.info(
                    "KIS 지수 조회 성공: {} → {}",
                    indexCode,
                    output.path("bstp_nmix_prpr").asText()
            );

            /**
             * MarketIndexDTO 생성
             * 프론트 메인 페이지 지수 카드 렌더링용
             */
            return new MarketIndexDTO(

                    // 코드값에 따른 지수명 변환
                    indexCode.equals("0001") ? "코스피" : "코스닥",

                    output.path("bstp_nmix_prpr").asText(), // 현재 지수

                    output.path("bstp_nmix_prdy_vrss").asText(), // 전일 대비

                    output.path("bstp_nmix_prdy_ctrt").asText(), // 등락률

                    output.path("bstp_nmix_oprc").asText(), // 시가

                    output.path("bstp_nmix_hgpr").asText(), // 고가

                    output.path("bstp_nmix_lwpr").asText() // 저가
            );

        } catch (Exception e) {

            log.error("KIS 지수 조회 실패: {} - {}", indexCode, e.getMessage());

            throw new RuntimeException("지수 조회 실패: " + e.getMessage());
        }
    }
}