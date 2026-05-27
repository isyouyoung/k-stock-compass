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

@Slf4j
@Service
@RequiredArgsConstructor
public class KisStockService {

    @Value("${kis.api.app-key}")
    private String appKey;

    @Value("${kis.api.app-secret}")
    private String appSecret;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final KisTokenService kisTokenService;

    /**
     * 종목 현재가 조회 (KIS 실전투자 API)
     */
    public long getCurrentPrice(String stockCode) {
        try {
            String token = kisTokenService.getAccessToken();

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

            // KIS 응답 코드 체크
            String rtCd = root.path("rt_cd").asText();
            if (!"0".equals(rtCd)) {
                String msg = root.path("msg1").asText();
                log.error("KIS API 비즈니스 로직 오류: {} (코드: {})", msg, rtCd);
                throw new RuntimeException("KIS API 오류: " + msg);
            }

            String price = root.path("output").path("stck_prpr").asText();
            log.info("KIS 현재가 조회 성공: {} → {}원", stockCode, price);
            return Long.parseLong(price);

        } catch (Exception e) {
            log.error("KIS 현재가 조회 실패: {} - {}", stockCode, e.getMessage());
            throw new RuntimeException("현재가 조회 실패: " + e.getMessage());
        }
    }

    public StockItemDTO getStockDetail(String stockCode) {
        try {
            String token = kisTokenService.getAccessToken();

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

            String rtCd = root.path("rt_cd").asText();
            if (!"0".equals(rtCd)) {
                String msg = root.path("msg1").asText();
                log.error("KIS API 오류: {} (코드: {})", msg, rtCd);
                throw new RuntimeException("KIS API 오류: " + msg);
            }

            JsonNode output = root.path("output");
            String price = output.path("stck_prpr").asText();   // 현재가
            String vs = output.path("prdy_vrss").asText();       // 전일대비
            String fltRt = output.path("prdy_ctrt").asText();    // 등락률

            log.info("KIS 상세 조회 성공: {} → {}원 ({}%)", stockCode, price, fltRt);

            return new StockItemDTO(
                    stockCode,
                    null,                                       // itmsNm
                    output.path("stck_prpr").asText(),          // clpr 현재가
                    output.path("prdy_ctrt").asText(),          // fltRt 등락률
                    output.path("prdy_vrss").asText(),          // vs 전일대비
                    null,                                       // mrktCtg
                    output.path("stck_oprc").asText(),          // oprc 시가
                    output.path("stck_hgpr").asText(),          // hgpr 고가
                    output.path("stck_lwpr").asText(),          // lwpr 저가
                    output.path("acml_vol").asText(),           // acmlVol 거래량
                    output.path("hts_avls").asText(),           // htsMktcap 시가총액
                    output.path("w52_hgpr").asText()            // w52Hgpr 52주 최고
            );

        } catch (Exception e) {
            log.error("KIS 상세 조회 실패: {} - {}", stockCode, e.getMessage());
            throw new RuntimeException("상세 조회 실패: " + e.getMessage());
        }
    }

    public MarketIndexDTO getIndexPrice(String indexCode) {
        try {
            String token = kisTokenService.getAccessToken();

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
                    .header("tr_id", "FHPUP02100000")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            String rtCd = root.path("rt_cd").asText();
            if (!"0".equals(rtCd)) {
                throw new RuntimeException("KIS 지수 API 오류: " + root.path("msg1").asText());
            }

            JsonNode output = root.path("output");
            log.info("KIS 지수 조회 성공: {} → {}", indexCode, output.path("bstp_nmix_prpr").asText());

            return new MarketIndexDTO(
                    indexCode.equals("0001") ? "코스피" : "코스닥",
                    output.path("bstp_nmix_prpr").asText(),   // 현재가
                    output.path("bstp_nmix_prdy_vrss").asText(), // 전일대비
                    output.path("bstp_nmix_prdy_ctrt").asText(), // 등락률
                    output.path("bstp_nmix_oprc").asText(),    // 시가
                    output.path("bstp_nmix_hgpr").asText(),    // 고가
                    output.path("bstp_nmix_lwpr").asText()     // 저가
            );

        } catch (Exception e) {
            log.error("KIS 지수 조회 실패: {} - {}", indexCode, e.getMessage());
            throw new RuntimeException("지수 조회 실패: " + e.getMessage());
        }
    }
}