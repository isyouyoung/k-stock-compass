package kopo.kstockcompass.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.kstockcompass.dto.MarketIndexDTO;
import kopo.kstockcompass.dto.StockItemDTO;
import kopo.kstockcompass.dto.StockSearchDTO;
import kopo.kstockcompass.service.IStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import kopo.kstockcompass.repository.entity.StockEntity;
import kopo.kstockcompass.repository.StockRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService implements IStockService {

    @Value("${public.api.stock.key}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final StockRepository stockRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * 주식 시세 조회 메서드 (Redis 캐싱 적용)
     * 역할: 종목코드 + 기준일자로 해당 종목의 시세를 조회합니다.
     * 흐름: Redis 확인 → 있으면 바로 반환 / 없으면 API 호출 → Redis 저장 → 반환
     * 특징: TTL 900초(15분) 캐싱으로 API 호출 횟수를 최소화합니다.
     */
    @Override
    public StockItemDTO getStockPrice(String stockCode, String baseDate) {

        String cacheKey = "stock:price:" + stockCode;

        try {
            // 1. Redis 캐시 확인
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.info("✅ Cache Hit! Redis에서 반환: {}", stockCode);
                return objectMapper.readValue(cached, StockItemDTO.class);
            }

            log.info("❌ Cache Miss! API 호출: {}", stockCode);

            // 2. 최근 영업일 자동 탐색 (최대 10일)
            for (int i = 0; i < 10; i++) {
                LocalDate date = LocalDate.parse(baseDate, DateTimeFormatter.ofPattern("yyyyMMdd"))
                        .minusDays(i);
                String targetDate = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

                String url = buildStockApiUrl(1, targetDate, 5000);
                String response = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                JsonNode root = objectMapper.readTree(response);
                String resultCode = root.path("response").path("header").path("resultCode").asText();
                if (!"00".equals(resultCode)) continue;

                JsonNode itemNode = root.path("response").path("body").path("items").path("item");
                if (!itemNode.isArray() || itemNode.isEmpty()) {
                    log.info("{}에 데이터 없음, 하루 전 재시도", targetDate);
                    continue;
                }

                for (JsonNode node : itemNode) {
                    String currentSrtnCd = node.path("srtnCd").asText();
                    if (stockCode.equals(currentSrtnCd)) {
                        StockItemDTO result = new StockItemDTO(
                                currentSrtnCd,
                                node.path("itmsNm").asText(),
                                node.path("clpr").asText(),
                                node.path("fltRt").asText(),
                                node.path("vs").asText(),
                                null, null, null, null, null, null, null
                        );

                        // 3. Redis 저장 (TTL 900초)
                        redisTemplate.opsForValue().set(
                                cacheKey,
                                objectMapper.writeValueAsString(result),
                                900,
                                TimeUnit.SECONDS
                        );
                        log.info("💾 Redis 저장 완료: {} (TTL 900초)", stockCode);
                        return result;
                    }
                }
            }

            throw new RuntimeException("최근 10일 내 " + stockCode + " 데이터를 찾을 수 없습니다.");

        } catch (Exception e) {
            log.error("Stock API 처리 중 에러 발생: {}", e.getMessage());
            throw new RuntimeException("주식 데이터 처리 실패: " + e.getMessage(), e);
        }
    }

    /**
     * [STEP 1] 초기화 전용 메서드
     * 역할: 기존 데이터를 싹 비우고 전체 수집을 시작합니다.
     */
    @Override
    @Transactional
    public void initStocks() {
        log.info("기존 종목 데이터를 삭제하고 초기화를 시작합니다.");
        stockRepository.deleteAllInBatch();
        saveAllStocks();
    }

    /**
     * [STEP 2] 전체 종목 수집 로직
     * 역할: 공공데이터 API에서 전체 종목을 받아와서 STOCK 테이블에 저장합니다.
     * 특징: saveAll()로 묶음 저장해서 성능을 높였습니다.
     */
    @Override
    public void saveAllStocks() {
        log.info("🔥 saveAllStocks 시작!"); // 로그 추가했음
        try {
            int pageNo = 1;
            int totalSaved = 0;
            String baseDate = "20260422";

            while (true) {
                String url = buildStockApiUrl(pageNo, baseDate, 1000);

                String response = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                JsonNode root = objectMapper.readTree(response);
                JsonNode items = root.path("response").path("body").path("items").path("item");

                if (!items.isArray() || items.isEmpty()) {
                    log.info("동기화 완료! 총 {}개 종목이 DB에 저장되었습니다.", totalSaved);
                    break;
                }

                // 묶음 저장 (성능 최적화)
                List<StockEntity> stockList = new ArrayList<>();
                for (JsonNode node : items) {
                    StockEntity stock = StockEntity.builder()
                            .stockCd(node.path("srtnCd").asText())
                            .stockNm(node.path("itmsNm").asText())
                            .mktType(node.path("mrktCtg").asText())
                            .build();
                    stockList.add(stock);
                }

                stockRepository.saveAll(stockList);
                totalSaved += stockList.size();
                log.info("{}페이지 저장 중... (누적: {}개)", pageNo, totalSaved);
                pageNo++;
            }

        } catch (Exception e) {
            log.error("전체 종목 저장 중 에러 발생: {}", e.getMessage());
            throw new RuntimeException("종목 수집 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 종목 검색 메서드
     * 역할: 사용자가 입력한 키워드로 STOCK 테이블에서 종목을 검색합니다.
     * 특징: DTO로 변환해서 필요한 필드만 반환합니다. (종목코드, 종목명)
     * 방어: null/공백 키워드 입력 시 빈 리스트 반환합니다.
     */
    @Override
    public List<StockSearchDTO> searchStocks(String query, String type) {

        // 방어 코드: 키워드가 null이거나 공백이면 빈 리스트 반환
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        // 앞뒤 공백 제거
        query = query.trim();
        log.debug("종목 검색 키워드: '{}', 타입: '{}'", query, type);

        return stockRepository.findByStockNmContaining(query)
                .stream()
                .map(stock -> new StockSearchDTO(
                        stock.getStockCd(),
                        stock.getStockNm()
                ))
                .toList();
    }

    /**
     * [공통] 공공데이터 API URL 빌더
     * 역할: URL 생성 로직을 공통화하여 유지보수성을 높입니다.
     * 특징: 한 곳에서 URL을 관리하므로 API 주소가 바뀌면 여기만 수정하면 됩니다.
     */
    private String buildStockApiUrl(int pageNo, String baseDate, int numOfRows) {
        return UriComponentsBuilder
                .fromHttpUrl("https://apis.data.go.kr/1160100/service/GetStockSecuritiesInfoService/getStockPriceInfo")
                .queryParam("serviceKey", apiKey)
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("resultType", "json")
                .queryParam("basDt", baseDate)
                .build(true)
                .toUriString();
    }

    @Override
    public MarketIndexDTO getMarketIndex(String idxNm, String baseDate) {

        // 최근 영업일 자동 탐색 (최대 10일 전까지)
        for (int i = 0; i < 10; i++) {
            try {
                LocalDate date = LocalDate.parse(baseDate, DateTimeFormatter.ofPattern("yyyyMMdd"))
                        .minusDays(i);
                String targetDate = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

                String url = UriComponentsBuilder
                        .fromHttpUrl("https://apis.data.go.kr/1160100/service/GetMarketIndexInfoService/getStockMarketIndex")
                        .queryParam("serviceKey", "5870f4586dba4a01c320f135f5cec6f13e06f50f2117f685eae1cf35b8ad6c1e")
                        .queryParam("numOfRows", 1)
                        .queryParam("pageNo", 1)
                        .queryParam("resultType", "json")
                        .queryParam("basDt", targetDate)
                        .queryParam("idxNm", idxNm)
                        .build(false)
                        .toUriString();

                String response = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                JsonNode root = objectMapper.readTree(response);
                JsonNode item = root.path("response").path("body").path("items").path("item");

                // 데이터 없으면 하루 더 과거로
                if (!item.isArray() || item.isEmpty()) {
                    log.info("{}에 {}데이터 없음, 하루 전 재시도", targetDate, idxNm);
                    continue;
                }

                JsonNode node = item.get(0);
                log.info("{}기준일 {}데이터 조회 성공", targetDate, idxNm);

                return new MarketIndexDTO(
                        node.path("idxNm").asText(),
                        node.path("clpr").asText(),
                        node.path("vs").asText(),
                        node.path("fltRt").asText(),
                        node.path("mkp").asText(),
                        node.path("hipr").asText(),
                        node.path("lopr").asText()
                );

            } catch (Exception e) {
                log.warn("{}일 조회 실패, 재시도: {}", i, e.getMessage());
            }
        }
        throw new RuntimeException("최근 10일 내 " + idxNm + " 데이터를 찾을 수 없습니다.");
    }

}