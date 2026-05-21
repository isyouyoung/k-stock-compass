package kopo.kstockcompass.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.kstockcompass.dto.FinancialDTO;
import kopo.kstockcompass.service.IFinancialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialService implements IFinancialService {

    @Value("${dart.api.key}")
    private String dartApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Override
    public FinancialDTO getFinancialData(String stockCode) {
        try {
            // 1. 종목코드 → corp_code 변환
            String corpCode = getCorpCode(stockCode);
            if (corpCode == null) {
                log.warn("corp_code 조회 실패: {}", stockCode);
                return null;
            }

            // 2. 재무제표 조회 (최근 연도 시도)
            String bsnsYear = "2024";
            JsonNode items = getFinancialItems(corpCode, bsnsYear);
            if (items == null || items.isEmpty()) {
                bsnsYear = "2023";
                items = getFinancialItems(corpCode, bsnsYear);
            }
            if (items == null || items.isEmpty()) {
                log.warn("재무데이터 없음: {}", stockCode);
                return null;
            }

            // 3. 항목 파싱
            BigDecimal currentAsset = null;
            BigDecimal currentLiab = null;
            BigDecimal totalLiab = null;
            BigDecimal totalEquity = null;
            BigDecimal revenue = null;
            BigDecimal operatingProfit = null;
            BigDecimal netIncome = null;

            for (JsonNode item : items) {
                String accountId = item.path("account_id").asText("");
                String accountNm = item.path("account_nm").asText("");
                String thstrmAmount = item.path("thstrm_amount").asText("").replaceAll(",", "");

                if (thstrmAmount.isEmpty() || thstrmAmount.equals("null")) continue;

                BigDecimal amount;
                try {
                    amount = new BigDecimal(thstrmAmount);
                } catch (Exception e) {
                    continue;
                }

                // 유동자산
                if (accountId.contains("CurrentAssets") || accountNm.equals("유동자산")) {
                    currentAsset = amount;
                }
                // 유동부채
                else if (accountId.contains("CurrentLiabilities") || accountNm.equals("유동부채")) {
                    currentLiab = amount;
                }
                // 부채총계
                else if (accountId.contains("Liabilities") && !accountId.contains("Current")
                        || accountNm.equals("부채총계")) {
                    totalLiab = amount;
                }
                // 자본총계
                else if (accountId.contains("Equity") && !accountId.contains("Non")
                        || accountNm.equals("자본총계")) {
                    totalEquity = amount;
                }
                // 매출액
                else if (accountId.contains("Revenue") || accountId.contains("Sales")
                        || accountNm.contains("매출액") || accountNm.contains("수익")) {
                    if (revenue == null) revenue = amount;
                }
                // 영업이익
                else if (accountId.contains("OperatingIncomeLoss") || accountId.contains("OperatingProfit")
                        || accountNm.contains("영업이익")) {
                    operatingProfit = amount;
                }
                // 당기순이익
                else if (accountId.contains("ProfitLoss") && !accountId.contains("Operating")
                        || accountNm.contains("당기순이익")) {
                    if (netIncome == null) netIncome = amount;
                }
            }

            // 4. 지표 계산
            // 부채비율
            BigDecimal debtRatio = null;
            if (totalEquity != null && totalLiab != null) {
                if (totalEquity.compareTo(BigDecimal.ZERO) <= 0) {
                    debtRatio = new BigDecimal("-1"); // 자본잠식 표시용
                } else {
                    debtRatio = totalLiab.divide(totalEquity, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(1, RoundingMode.HALF_UP);
                }
            }

            // 영업이익률
            BigDecimal operatingMargin = null;
            if (revenue != null && operatingProfit != null
                    && revenue.compareTo(BigDecimal.ZERO) != 0) {
                operatingMargin = operatingProfit.divide(revenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP);
            }

            // 유동비율
            BigDecimal currentRatio = null;
            if (currentAsset != null && currentLiab != null
                    && currentLiab.compareTo(BigDecimal.ZERO) != 0) {
                currentRatio = currentAsset.divide(currentLiab, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP);
            }

            log.info("DART 재무 조회 성공: {} ({}년)", stockCode, bsnsYear);

            return FinancialDTO.builder()
                    .stockCd(stockCode)
                    .bsnsYear(bsnsYear)
                    .currentAsset(currentAsset)
                    .currentLiab(currentLiab)
                    .totalLiab(totalLiab)
                    .totalEquity(totalEquity)
                    .revenue(revenue)
                    .operatingProfit(operatingProfit)
                    .netIncome(netIncome)
                    .debtRatio(debtRatio)
                    .operatingMargin(operatingMargin)
                    .currentRatio(currentRatio)
                    .build();

        } catch (Exception e) {
            log.error("DART 재무 조회 실패: {} - {}", stockCode, e.getMessage());
            return null;
        }
    }

    private String getCorpCode(String stockCode) {
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("opendart.fss.or.kr")
                            .path("/api/company.json")
                            .queryParam("crtfc_key", dartApiKey)
                            .queryParam("stock_code", stockCode)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            if ("000".equals(root.path("status").asText())) {
                return root.path("corp_code").asText();
            }
            return null;
        } catch (Exception e) {
            log.error("corp_code 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    private JsonNode getFinancialItems(String corpCode, String bsnsYear) {
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("opendart.fss.or.kr")
                            .path("/api/fnlttSinglAcntAll.json")
                            .queryParam("crtfc_key", dartApiKey)
                            .queryParam("corp_code", corpCode)
                            .queryParam("bsns_year", bsnsYear)
                            .queryParam("reprt_code", "11011") // 사업보고서
                            .queryParam("fs_div", "CFS") // 연결재무제표
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            if ("000".equals(root.path("status").asText())) {
                return root.path("list");
            }
            // 연결재무제표 없으면 별도재무제표 시도
            response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("opendart.fss.or.kr")
                            .path("/api/fnlttSinglAcntAll.json")
                            .queryParam("crtfc_key", dartApiKey)
                            .queryParam("corp_code", corpCode)
                            .queryParam("bsns_year", bsnsYear)
                            .queryParam("reprt_code", "11011")
                            .queryParam("fs_div", "OFS") // 별도재무제표
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            root = objectMapper.readTree(response);
            if ("000".equals(root.path("status").asText())) {
                return root.path("list");
            }
            return null;
        } catch (Exception e) {
            log.error("재무데이터 조회 실패: {}", e.getMessage());
            return null;
        }
    }
}