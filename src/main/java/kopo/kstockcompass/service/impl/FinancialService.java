package kopo.kstockcompass.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.kstockcompass.dto.FinancialDTO;
import kopo.kstockcompass.service.IFinancialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * [DART 재무정보 분석 서비스]
 *
 * 설명:
 * 금융감독원 DART Open API를 활용하여
 * 특정 종목의 재무제표 데이터를 조회하고,
 * 핵심 재무 비율(부채비율, 영업이익률, 유동비율 등)을 계산하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialService implements IFinancialService {

    // DART Open API 인증키
    @Value("${dart.api.key}")
    private String dartApiKey;

    // JSON 파싱 처리용 ObjectMapper
    private final ObjectMapper objectMapper;

    /**
     * [재무정보 조회 메인 로직]
     */
    @Override
    public FinancialDTO getFinancialData(String stockCode) {

        try {

            // 1단계: 종목코드(stockCode)를 DART 고유 corp_code로 변환
            String corpCode = getCorpCode(stockCode);

            // corp_code 조회 실패 시 종료
            if (corpCode == null) {
                log.warn("corp_code 조회 실패: {}", stockCode);
                return null;
            }

            /**
             * 2단계: 재무제표 조회
             * 우선 최신 연도(2024)를 시도하고,
             * 데이터가 없으면 이전 연도(2023)를 재시도합니다.
             */
            String bsnsYear = "2024";

            JsonNode items = getFinancialItems(corpCode, bsnsYear);

            if (items == null || items.isEmpty()) {

                // fallback 전략
                bsnsYear = "2023";

                items = getFinancialItems(corpCode, bsnsYear);
            }

            // 최종적으로 데이터가 없으면 종료
            if (items == null || items.isEmpty()) {

                log.warn("재무데이터 없음: {}", stockCode);

                return null;
            }

            /**
             * 3단계: 주요 재무 항목 변수 선언
             */
            BigDecimal currentAsset = null;      // 유동자산
            BigDecimal currentLiab = null;       // 유동부채
            BigDecimal totalLiab = null;         // 부채총계
            BigDecimal totalEquity = null;       // 자본총계
            BigDecimal revenue = null;           // 매출액
            BigDecimal operatingProfit = null;   // 영업이익
            BigDecimal netIncome = null;         // 당기순이익

            /**
             * 4단계: 재무제표 항목 순회 분석
             */
            for (JsonNode item : items) {

                String accountId = item.path("account_id").asText("");
                String accountNm = item.path("account_nm").asText("");

                String thstrmAmount = item.path("thstrm_amount")
                        .asText("")
                        .replaceAll(",", "");

                if (thstrmAmount.isEmpty() || thstrmAmount.equals("null")) {
                    continue;
                }

                BigDecimal amount;

                try {
                    amount = new BigDecimal(thstrmAmount);
                } catch (Exception e) {
                    continue;
                }

                // 유동자산
                if ("ifrs-full_CurrentAssets".equals(accountId)
                        || "유동자산".equals(accountNm)) {
                    currentAsset = amount;
                }
                // 유동부채
                else if ("ifrs-full_CurrentLiabilities".equals(accountId)
                        || "유동부채".equals(accountNm)) {
                    currentLiab = amount;
                }
                // 부채총계
                else if ("ifrs-full_Liabilities".equals(accountId)
                        || "부채총계".equals(accountNm)) {
                    totalLiab = amount;
                }
                // 자본총계
                else if ("ifrs-full_Equity".equals(accountId)
                        && "자본총계".equals(accountNm)) {
                    if (totalEquity == null) {
                        totalEquity = amount;
                    }
                }
                // 매출액
                else if ("ifrs-full_Revenue".equals(accountId)
                        || "dart_Revenues".equals(accountId)
                        || "매출액".equals(accountNm)) {
                    if (revenue == null) {
                        revenue = amount;
                    }
                }
                // 영업이익
                else if ("dart_OperatingIncomeLoss".equals(accountId)
                        || "ifrs-full_ProfitLossFromOperatingActivities".equals(accountId)
                        || "영업이익".equals(accountNm)
                        || "영업이익(손실)".equals(accountNm)) {
                    operatingProfit = amount;
                }
                // 당기순이익
                else if ("ifrs-full_ProfitLoss".equals(accountId)
                        || "당기순이익".equals(accountNm)
                        || "당기순이익(손실)".equals(accountNm)) {
                    if (netIncome == null) {
                        netIncome = amount;
                    }
                }
            }

            /**
             * 5단계: 재무비율 계산
             */
            BigDecimal debtRatio = null;

            if (totalEquity != null && totalLiab != null) {

                if (totalEquity.compareTo(BigDecimal.ZERO) <= 0) {
                    debtRatio = new BigDecimal("-1");
                } else {
                    debtRatio = totalLiab
                            .divide(totalEquity, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(1, RoundingMode.HALF_UP);
                }
            }

            BigDecimal operatingMargin = null;

            if (revenue != null
                    && operatingProfit != null
                    && revenue.compareTo(BigDecimal.ZERO) != 0) {

                operatingMargin = operatingProfit
                        .divide(revenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP);
            }

            BigDecimal currentRatio = null;

            if (currentAsset != null
                    && currentLiab != null
                    && currentLiab.compareTo(BigDecimal.ZERO) != 0) {

                currentRatio = currentAsset
                        .divide(currentLiab, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP);
            }

            log.info("DART 재무 조회 성공: {} ({}년)", stockCode, bsnsYear);

            return new FinancialDTO(
                    stockCode,
                    null,
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

    /**
     * [stockCode → corp_code 변환]
     * WebClient SSL 핸드셰이크 이슈 해결을 위해 RestTemplate 기반으로 다운로드
     */
    private String getCorpCode(String stockCode) {

        try {

            String url = "https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key=" + dartApiKey;

            // RestTemplate으로 통신하여 JDK 기본 SSL 엔진 사용
            RestTemplate restTemplate = new RestTemplate();
            byte[] zipBytes = restTemplate.getForObject(url, byte[].class);

            if (zipBytes == null) {
                return null;
            }

            String xmlContent = null;

            try (java.util.zip.ZipInputStream zis =
                         new java.util.zip.ZipInputStream(
                                 new java.io.ByteArrayInputStream(zipBytes))) {

                java.util.zip.ZipEntry entry;

                while ((entry = zis.getNextEntry()) != null) {

                    if (entry.getName().endsWith(".xml")) {

                        xmlContent = new String(zis.readAllBytes(), "UTF-8");

                        break;
                    }
                }
            }

            if (xmlContent == null) {
                return null;
            }

            String searchStr = "<stock_code>" + stockCode + "</stock_code>";

            int idx = xmlContent.indexOf(searchStr);

            if (idx == -1) {
                return null;
            }

            int start = xmlContent.lastIndexOf("<corp_code>", idx);

            int end = xmlContent.indexOf("</corp_code>", start);

            if (start == -1 || end == -1) {
                return null;
            }

            String corpCode = xmlContent.substring(start + "<corp_code>".length(), end);

            log.info("corp_code 조회 성공: {} → {}", stockCode, corpCode);

            return corpCode;

        } catch (Exception e) {

            log.error("corp_code 조회 실패: {}", e.getMessage());

            return null;
        }
    }

    /**
     * [DART 재무제표 조회]
     * RestTemplate 기반 통신 적용
     */
    private JsonNode getFinancialItems(String corpCode, String bsnsYear) {

        try {

            RestTemplate restTemplate = new RestTemplate();

            // 연결재무제표(CFS) 조회
            String cfsUrl = String.format(
                    "https://opendart.fss.or.kr/api/fnlttSinglAcntAll.json?crtfc_key=%s&corp_code=%s&bsns_year=%s&reprt_code=11011&fs_div=CFS",
                    dartApiKey, corpCode, bsnsYear
            );

            String response = restTemplate.getForObject(cfsUrl, String.class);
            JsonNode root = objectMapper.readTree(response);

            if ("000".equals(root.path("status").asText())) {
                return root.path("list");
            }

            // 별도재무제표(OFS) 조회
            String ofsUrl = String.format(
                    "https://opendart.fss.or.kr/api/fnlttSinglAcntAll.json?crtfc_key=%s&corp_code=%s&bsns_year=%s&reprt_code=11011&fs_div=OFS",
                    dartApiKey, corpCode, bsnsYear
            );

            response = restTemplate.getForObject(ofsUrl, String.class);
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