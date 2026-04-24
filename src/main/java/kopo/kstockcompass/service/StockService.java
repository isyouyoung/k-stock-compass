package kopo.kstockcompass.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.kstockcompass.dto.StockItemDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    @Value("${public.api.stock.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public StockItemDTO getStockPrice(String stockCode, String baseDate) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://apis.data.go.kr/1160100/service/GetStockSecuritiesInfoService/getStockPriceInfo")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("numOfRows", 1000)
                    .queryParam("pageNo", 1)
                    .queryParam("resultType", "json")
                    .queryParam("basDt", baseDate)
                    .build(true)
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            // API 응답 코드 검증
            String resultCode = root.path("response").path("header").path("resultCode").asText();
            if (!"00".equals(resultCode)) {
                String resultMsg = root.path("response").path("header").path("resultMsg").asText();
                throw new RuntimeException("공공데이터 API 오류 [" + resultCode + "]: " + resultMsg);
            }

            JsonNode itemNode = root.path("response").path("body").path("items").path("item");

            // 데이터 유무 확인
            if (!itemNode.isArray() || itemNode.isEmpty()) {
                throw new RuntimeException("해당 날짜(" + baseDate + ")에 데이터가 없습니다.");
            }

            // 종목코드로 필터링
            for (JsonNode node : itemNode) {
                String currentSrtnCd = node.path("srtnCd").asText();
                if (stockCode != null && stockCode.equals(currentSrtnCd)) {
                    log.debug("종목 매칭 성공: {} ({})", node.path("itmsNm").asText(), stockCode);
                    return StockItemDTO.builder()
                            .srtnCd(currentSrtnCd)
                            .itmsNm(node.path("itmsNm").asText())
                            .clpr(node.path("clpr").asText())
                            .fltRt(node.path("fltRt").asText())
                            .vs(node.path("vs").asText())
                            .build();
                }
            }

            throw new RuntimeException("해당 종목(" + stockCode + ")이 없습니다.");

        } catch (Exception e) {
            log.error("Stock API 처리 중 에러 발생: {}", e.getMessage());
            throw new RuntimeException("주식 데이터 처리 실패: " + e.getMessage(), e);
        }
    }
}