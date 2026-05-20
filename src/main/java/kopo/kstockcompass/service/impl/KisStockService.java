package kopo.kstockcompass.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

            return StockItemDTO.builder()
                    .srtnCd(stockCode)
                    .clpr(output.path("stck_prpr").asText())   // 현재가
                    .vs(output.path("prdy_vrss").asText())      // 전일대비
                    .fltRt(output.path("prdy_ctrt").asText())   // 등락률
                    .oprc(output.path("stck_oprc").asText())    // 시가
                    .hgpr(output.path("stck_hgpr").asText())    // 고가
                    .lwpr(output.path("stck_lwpr").asText())    // 저가
                    .acmlVol(output.path("acml_vol").asText())  // 거래량
                    .htsMktcap(output.path("hts_avls").asText()) // 시가총액
                    .w52Hgpr(output.path("w52_hgpr").asText())  // 52주 최고
                    .build();

        } catch (Exception e) {
            log.error("KIS 상세 조회 실패: {} - {}", stockCode, e.getMessage());
            throw new RuntimeException("상세 조회 실패: " + e.getMessage());
        }
    }
}