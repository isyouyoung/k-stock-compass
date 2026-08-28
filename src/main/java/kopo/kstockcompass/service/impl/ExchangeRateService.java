package kopo.kstockcompass.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.kstockcompass.dto.ExchangeRateResponseDto;
import kopo.kstockcompass.dto.ExchangeRateRowDto;
import kopo.kstockcompass.dto.StatisticSearchDto;
import kopo.kstockcompass.dto.StatisticSearchResponseDto;
import kopo.kstockcompass.service.IExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService implements IExchangeRateService {

    private final WebClient webClient;
    private final StringRedisTemplate redisTemplate; // 기존 Redis 연결 활용
    private final ObjectMapper objectMapper;         // JSON 직렬화/역직렬화용

    @Value("${bok.api.key}")
    private String apiKey;

    // Redis 캐시 Key 및 TTL(유효기간 24시간) 설정
    private static final String REDIS_KEY = "exchange_rate_cache";
    private static final long CACHE_TTL_HOURS = 24;

    // 조회할 통화 목록 (8개국)
    private static final List<CurrencyTarget> TARGETS = List.of(
            new CurrencyTarget("미국 달러", "731Y001", "0000001"),
            new CurrencyTarget("일본 엔 (100엔)", "731Y001", "0000002"),
            new CurrencyTarget("유로", "731Y001", "0000003"),
            new CurrencyTarget("중국 위안", "731Y001", "0000053"),
            new CurrencyTarget("영국 파운드", "731Y001", "0000012"),
            new CurrencyTarget("호주 달러", "731Y001", "0000017"),
            new CurrencyTarget("캐나다 달러", "731Y001", "0000013"),
            new CurrencyTarget("스위스 프랑", "731Y001", "0000014")
    );

    @Override
    public List<ExchangeRateResponseDto> getExchangeRates() {

        try {
            // 1. Redis에서 저장된 JSON 문자열 캐시 조회
            String cachedJson = redisTemplate.opsForValue().get(REDIS_KEY);

            // 2. Redis 캐시에 데이터가 존재하면 JSON -> List<ExchangeRateResponseDto> 역직렬화 후 반환 (Cache Hit)
            if (StringUtils.hasText(cachedJson)) {
                log.info("⚡ [Redis Cache Hit] Redis에 저장된 환율 정보를 즉시 반환합니다.");
                return objectMapper.readValue(cachedJson, new TypeReference<List<ExchangeRateResponseDto>>() {});
            }
        } catch (Exception e) {
            log.error("Redis 캐시 조회 실패 - ECOS API 직접 호출로 Fallback: {}", e.getMessage());
        }

        // 3. Redis 캐시가 비어 있거나 오류 발생 시 (Cache Miss) ECOS API 호출 및 결과 즉시 반환
        log.info("❄️ [Redis Cache Miss] Redis 캐시가 비어있어 ECOS API를 호출합니다.");
        return refreshExchangeRateCache();
    }

    /**
     * 매일 오전 9시에 환율 Redis 캐시 자동 갱신
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void autoRefreshExchangeRateCache() {
        log.info("🪙 오늘의 환율 정보가 새로 업데이트되었습니다 (Redis 캐시 갱신).");
        refreshExchangeRateCache();
    }

    /**
     * 한국은행 ECOS API에서 환율 데이터를 조회하여
     * Redis 캐시를 갱신하고, 가공된 데이터를 반환한다.
     */
    public List<ExchangeRateResponseDto> refreshExchangeRateCache() {

        List<ExchangeRateResponseDto> newRates = new ArrayList<>();

        try {
            // 최근 7일 데이터를 조회 (주말 및 공휴일 대비)
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(7);

            String endDateStr = endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String startDateStr = startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            for (CurrencyTarget target : TARGETS) {

                String url = String.format(
                        "https://ecos.bok.or.kr/api/StatisticSearch/%s/json/kr/1/10/%s/D/%s/%s/%s",
                        apiKey,
                        target.statCode,
                        startDateStr,
                        endDateStr,
                        target.itemCode
                );

                log.info("ECOS 환율 조회 - 통화: {}, 기간: {} ~ {}", target.name, startDateStr, endDateStr);

                StatisticSearchResponseDto response = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(StatisticSearchResponseDto.class)
                        .block();

                // ✨ Optional 체이닝으로 NPE 차단 및 빈 리스트 가공
                List<ExchangeRateRowDto> rows = Optional.ofNullable(response)
                        .map(StatisticSearchResponseDto::statisticSearch)
                        .map(StatisticSearchDto::row)
                        .orElse(Collections.emptyList());

                // 통합 방어 로직 및 로그 기록
                if (rows.isEmpty()) {
                    log.warn("ECOS 환율 응답 데이터가 없거나 올바르지 않습니다. 통화: {}", target.name);
                    continue;
                }

                ExchangeRateRowDto latestRow = rows.get(rows.size() - 1);
                double basePrice = Double.parseDouble(latestRow.dataValue());
                double prevPrice = basePrice;

                if (rows.size() >= 2) {
                    ExchangeRateRowDto previousRow = rows.get(rows.size() - 2);
                    prevPrice = Double.parseDouble(previousRow.dataValue());
                }

                double changePrice = basePrice - prevPrice;
                double changeRate = (prevPrice == 0) ? 0.0 : (changePrice / prevPrice) * 100;

                String change = "EVEN";
                if (changePrice > 0) {
                    change = "RISE";
                } else if (changePrice < 0) {
                    change = "FALL";
                }

                ExchangeRateResponseDto dto = ExchangeRateResponseDto.builder()
                        .currency(target.itemCode)
                        .name(target.name)
                        .rate(String.format("%,.2f", basePrice))
                        .change(change)
                        .changePrice(String.format("%+.2f", changePrice))
                        .changeRate(String.format("%+.2f%%", changeRate))
                        .build();

                newRates.add(dto);

                log.info("환율 처리 완료 - {} : {}", target.name, basePrice);
            }

            // ✨ 빈 배열 캐싱 방지: 정상적으로 수집된 데이터가 존재할 때만 Redis에 저장
            if (!newRates.isEmpty()) {
                String jsonString = objectMapper.writeValueAsString(newRates);
                redisTemplate.opsForValue().set(REDIS_KEY, jsonString, CACHE_TTL_HOURS, TimeUnit.HOURS);
                log.info("⏰ [Redis 캐시] 한국은행 ECOS 환율 정보 갱신 완료 (총 {}개 통화)", newRates.size());
            }

        } catch (Exception e) {
            log.error("한국은행 ECOS 환율 Redis 캐시 갱신 중 오류 발생", e);
        }

        return newRates;
    }

    /**
     * 환율 조회 대상
     */
    private static class CurrencyTarget {

        String name;
        String statCode;
        String itemCode;

        public CurrencyTarget(String name, String statCode, String itemCode) {
            this.name = name;
            this.statCode = statCode;
            this.itemCode = itemCode;
        }
    }
}