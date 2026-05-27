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

                // 유동자산 - 정확히 매칭
                if ("ifrs-full_CurrentAssets".equals(accountId) || "유동자산".equals(accountNm)) {
                    currentAsset = amount;
                }
                // 유동부채 - 정확히 매칭
                else if ("ifrs-full_CurrentLiabilities".equals(accountId) || "유동부채".equals(accountNm)) {
                    currentLiab = amount;
                }
                // 부채총계 - 정확히 매칭
                else if ("ifrs-full_Liabilities".equals(accountId) || "부채총계".equals(accountNm)) {
                    totalLiab = amount;
                }
                // 자본총계 - 처음 잡힌 값만 사용 (첫 번째가 자본총계)
                else if ("ifrs-full_Equity".equals(accountId) && "자본총계".equals(accountNm)) {
                    if (totalEquity == null) totalEquity = amount; // 처음 한 번만!
                }
                // 매출액
                else if ("ifrs-full_Revenue".equals(accountId)
                        || "dart_Revenues".equals(accountId)
                        || "매출액".equals(accountNm)) {
                    if (revenue == null) revenue = amount;
                }
                // 영업이익
                else if ("dart_OperatingIncomeLoss".equals(accountId)
                        || "ifrs-full_ProfitLossFromOperatingActivities".equals(accountId)
                        || "영업이익".equals(accountNm) || "영업이익(손실)".equals(accountNm)) {
                    operatingProfit = amount;
                }
                // 당기순이익
                else if ("ifrs-full_ProfitLoss".equals(accountId)
                        || "당기순이익".equals(accountNm) || "당기순이익(손실)".equals(accountNm)) {
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

            return new FinancialDTO(
                    stockCode,
                    null,               // corpName
                    bsnsYear,
                    currentAsset,
                    currentLiab,
                    totalLiab,
                    totalEquity,
                    revenue,
                    operatingProfit,
                    netIncome,
                    debtRatio,
                    operatingMargin,
                    currentRatio
            );

        } catch (Exception e) {
            log.error("DART 재무 조회 실패: {} - {}", stockCode, e.getMessage());
            return null;
        }
    }

    private String getCorpCode(String stockCode) {
        try {
            byte[] zipBytes = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("opendart.fss.or.kr")
                            .path("/api/corpCode.xml")
                            .queryParam("crtfc_key", dartApiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            if (zipBytes == null) return null;

            // ZIP 압축 해제
            String xmlContent = null;
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                    new java.io.ByteArrayInputStream(zipBytes))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().endsWith(".xml")) {
                        xmlContent = new String(zis.readAllBytes(), "UTF-8");
                        break;
                    }
                }
            }

            if (xmlContent == null) return null;

            // XML에서 stock_code로 corp_code 찾기
            String searchStr = "<stock_code>" + stockCode + "</stock_code>";
            int idx = xmlContent.indexOf(searchStr);
            if (idx == -1) return null;

            int start = xmlContent.lastIndexOf("<corp_code>", idx);
            int end = xmlContent.indexOf("</corp_code>", start);
            if (start == -1 || end == -1) return null;

            String corpCode = xmlContent.substring(start + "<corp_code>".length(), end);
            log.info("corp_code 조회 성공: {} → {}", stockCode, corpCode);
            return corpCode;

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
                JsonNode list = root.path("list");
                return list;
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