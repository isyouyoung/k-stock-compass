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

/**
 * [DART 재무정보 분석 서비스]
 *
 * 설명:
 * 금융감독원 DART Open API를 활용하여
 * 특정 종목의 재무제표 데이터를 조회하고,
 * 핵심 재무 비율(부채비율, 영업이익률, 유동비율 등)을 계산하는 서비스입니다.
 *
 * 주요 기능:
 * 1. 종목코드(stockCode) → DART corp_code 변환
 * 2. 재무제표 데이터 조회
 * 3. 핵심 계정과목 추출
 * 4. 재무 비율 계산
 * 5. DTO 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialService implements IFinancialService {

    // DART Open API 인증키
    @Value("${dart.api.key}")
    private String dartApiKey;

    // 외부 API 통신용 WebClient
    private final WebClient webClient;

    // JSON 파싱 처리용 ObjectMapper
    private final ObjectMapper objectMapper;

    /**
     * [재무정보 조회 메인 로직]
     *
     * 역할:
     * 특정 종목의 재무 데이터를 조회하고,
     * 핵심 재무 지표를 계산하여 반환합니다.
     *
     * 처리 흐름:
     * 1. stockCode → corp_code 변환
     * 2. DART 재무제표 조회
     * 3. 계정과목 파싱
     * 4. 재무비율 계산
     * 5. DTO 반환
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
             *
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
             *
             * BigDecimal 사용 이유:
             * 금융 데이터는 부동소수점 오차를 방지해야 하므로
             * double 대신 BigDecimal 사용
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
             *
             * DART 응답은 수많은 계정과목(account_id)을 포함하므로
             * 필요한 계정만 직접 추출합니다.
             */
            for (JsonNode item : items) {

                // 표준 계정 ID
                String accountId = item.path("account_id").asText("");

                // 한글 계정명
                String accountNm = item.path("account_nm").asText("");

                // 금액 문자열 추출
                String thstrmAmount = item.path("thstrm_amount")
                        .asText("")
                        .replaceAll(",", "");

                // 데이터 없으면 skip
                if (thstrmAmount.isEmpty() || thstrmAmount.equals("null")) {
                    continue;
                }

                BigDecimal amount;

                try {

                    // 문자열 → BigDecimal 변환
                    amount = new BigDecimal(thstrmAmount);

                } catch (Exception e) {

                    // 숫자 변환 실패 시 skip
                    continue;
                }

                /**
                 * 유동자산(Current Assets)
                 */
                if ("ifrs-full_CurrentAssets".equals(accountId)
                        || "유동자산".equals(accountNm)) {

                    currentAsset = amount;
                }

                /**
                 * 유동부채(Current Liabilities)
                 */
                else if ("ifrs-full_CurrentLiabilities".equals(accountId)
                        || "유동부채".equals(accountNm)) {

                    currentLiab = amount;
                }

                /**
                 * 부채총계(Total Liabilities)
                 */
                else if ("ifrs-full_Liabilities".equals(accountId)
                        || "부채총계".equals(accountNm)) {

                    totalLiab = amount;
                }

                /**
                 * 자본총계(Equity)
                 *
                 * 동일 계정이 여러 번 등장할 수 있으므로
                 * 최초 값만 사용
                 */
                else if ("ifrs-full_Equity".equals(accountId)
                        && "자본총계".equals(accountNm)) {

                    if (totalEquity == null) {
                        totalEquity = amount;
                    }
                }

                /**
                 * 매출액(Revenue)
                 */
                else if ("ifrs-full_Revenue".equals(accountId)
                        || "dart_Revenues".equals(accountId)
                        || "매출액".equals(accountNm)) {

                    if (revenue == null) {
                        revenue = amount;
                    }
                }

                /**
                 * 영업이익(Operating Profit)
                 */
                else if ("dart_OperatingIncomeLoss".equals(accountId)
                        || "ifrs-full_ProfitLossFromOperatingActivities".equals(accountId)
                        || "영업이익".equals(accountNm)
                        || "영업이익(손실)".equals(accountNm)) {

                    operatingProfit = amount;
                }

                /**
                 * 당기순이익(Net Income)
                 */
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

            /**
             * 부채비율 계산
             *
             * 공식:
             * 부채총계 ÷ 자본총계 × 100
             */
            BigDecimal debtRatio = null;

            if (totalEquity != null && totalLiab != null) {

                // 자본잠식 방어 처리
                if (totalEquity.compareTo(BigDecimal.ZERO) <= 0) {

                    debtRatio = new BigDecimal("-1");

                } else {

                    debtRatio = totalLiab
                            .divide(totalEquity, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(1, RoundingMode.HALF_UP);
                }
            }

            /**
             * 영업이익률 계산
             *
             * 공식:
             * 영업이익 ÷ 매출액 × 100
             */
            BigDecimal operatingMargin = null;

            if (revenue != null
                    && operatingProfit != null
                    && revenue.compareTo(BigDecimal.ZERO) != 0) {

                operatingMargin = operatingProfit
                        .divide(revenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP);
            }

            /**
             * 유동비율 계산
             *
             * 공식:
             * 유동자산 ÷ 유동부채 × 100
             */
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

            /**
             * 6단계: DTO 반환
             */
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
     *
     * 설명:
     * DART API는 일반 종목코드가 아닌
     * 고유 corp_code를 사용합니다.
     *
     * 따라서 corpCode.xml 파일을 다운로드 후
     * XML 내부에서 stock_code를 검색하여
     * 대응되는 corp_code를 추출합니다.
     */
    private String getCorpCode(String stockCode) {

        try {

            // ZIP 형태 XML 다운로드
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

            if (zipBytes == null) {
                return null;
            }

            /**
             * ZIP 압축 해제
             */
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

            /**
             * XML 문자열 내부에서 stock_code 검색
             */
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

            String corpCode =
                    xmlContent.substring(start + "<corp_code>".length(), end);

            log.info("corp_code 조회 성공: {} → {}", stockCode, corpCode);

            return corpCode;

        } catch (Exception e) {

            log.error("corp_code 조회 실패: {}", e.getMessage());

            return null;
        }
    }

    /**
     * [DART 재무제표 조회]
     *
     * 설명:
     * 우선 연결재무제표(CFS)를 시도하고,
     * 없으면 별도재무제표(OFS)를 fallback 전략으로 조회합니다.
     */
    private JsonNode getFinancialItems(String corpCode, String bsnsYear) {

        try {

            /**
             * 연결재무제표(CFS) 조회
             */
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("opendart.fss.or.kr")
                            .path("/api/fnlttSinglAcntAll.json")
                            .queryParam("crtfc_key", dartApiKey)
                            .queryParam("corp_code", corpCode)
                            .queryParam("bsns_year", bsnsYear)
                            .queryParam("reprt_code", "11011")
                            .queryParam("fs_div", "CFS")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);

            // 정상 응답 여부 확인
            if ("000".equals(root.path("status").asText())) {

                return root.path("list");
            }

            /**
             * 연결재무제표가 없으면
             * 별도재무제표(OFS) 조회
             */
            response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("opendart.fss.or.kr")
                            .path("/api/fnlttSinglAcntAll.json")
                            .queryParam("crtfc_key", dartApiKey)
                            .queryParam("corp_code", corpCode)
                            .queryParam("bsns_year", bsnsYear)
                            .queryParam("reprt_code", "11011")
                            .queryParam("fs_div", "OFS")
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