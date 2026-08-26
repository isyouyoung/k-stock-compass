package kopo.kstockcompass.service.impl;

import kopo.kstockcompass.dto.ExchangeRateResponseDto;
import kopo.kstockcompass.dto.ExchangeRateRowDto;
import kopo.kstockcompass.dto.StatisticSearchResponseDto;
import kopo.kstockcompass.service.IExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService implements IExchangeRateService {

    private final WebClient webClient;

    @Value("${bok.api.key}")
    private String apiKey;

    // Java 메모리 캐시 저장소
    // 여러 요청이 동시에 들어와도 안전하게 데이터를 교체하기 위해 AtomicReference 사용
    private final AtomicReference<List<ExchangeRateResponseDto>> cachedRates =
            new AtomicReference<>(new ArrayList<>());

    // 조회할 통화 목록 (8개국으로 확장)
    // 이름, ECOS 통계표 코드, ECOS 항목 코드
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

        List<ExchangeRateResponseDto> rates = cachedRates.get();

        // 캐시가 비어 있으면 즉시 ECOS에서 데이터를 가져옴
        if (rates.isEmpty()) {
            refreshExchangeRateCache();
            return cachedRates.get();
        }

        return rates;
    }

    /**
     * 매일 오전 9시에 환율 캐시 자동 갱신
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void autoRefreshExchangeRateCache() {
        log.info("\uD83D\uDCB1 오늘의 환율 정보가 새로 업데이트되었습니다.");
        refreshExchangeRateCache();
    }

    /**
     * 한국은행 ECOS API에서 환율 데이터를 조회하여
     * Java 메모리 캐시를 갱신한다.
     */
    public void refreshExchangeRateCache() {

        try {

            List<ExchangeRateResponseDto> newRates = new ArrayList<>();

            // 최근 7일 데이터를 조회
            // 주말이나 공휴일에는 데이터가 없을 수 있기 때문에
            // 넉넉하게 7일을 조회한다.
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(7);

            String endDateStr =
                    endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            String startDateStr =
                    startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 8개 통화 순서대로 조회
            for (CurrencyTarget target : TARGETS) {

                // ECOS StatisticSearch API URL
                String url = String.format(
                        "https://ecos.bok.or.kr/api/StatisticSearch/%s/json/kr/1/10/%s/D/%s/%s/%s",
                        apiKey,
                        target.statCode,
                        startDateStr,
                        endDateStr,
                        target.itemCode
                );

                log.info(
                        "ECOS 환율 조회 - 통화: {}, 기간: {} ~ {}",
                        target.name,
                        startDateStr,
                        endDateStr
                );

                // ECOS JSON 응답을 DTO로 바로 변환
                StatisticSearchResponseDto response = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(StatisticSearchResponseDto.class)
                        .block();

                // 응답이 비어있는 경우 방어
                if (response == null || response.statisticSearch() == null) {
                    log.warn(
                            "ECOS 환율 응답이 없습니다. 통화: {}",
                            target.name
                    );
                    continue;
                }

                // StatisticSearch 내부의 row 목록
                List<ExchangeRateRowDto> rows =
                        response.statisticSearch().row();

                // 조회된 데이터가 없는 경우 해당 통화는 건너뜀
                if (rows == null || rows.isEmpty()) {
                    log.warn(
                            "ECOS 환율 데이터가 없습니다. 통화: {}",
                            target.name
                    );
                    continue;
                }

                // 가장 최근 데이터
                ExchangeRateRowDto latestRow =
                        rows.get(rows.size() - 1);

                double basePrice =
                        Double.parseDouble(latestRow.dataValue());

                // 기본값은 현재 환율과 동일하게 설정
                double prevPrice = basePrice;

                // 직전 데이터가 존재하면 직전 데이터를 사용
                if (rows.size() >= 2) {

                    ExchangeRateRowDto previousRow =
                            rows.get(rows.size() - 2);

                    prevPrice =
                            Double.parseDouble(previousRow.dataValue());
                }

                // 전일 대비 금액
                double changePrice =
                        basePrice - prevPrice;

                // 전일 대비 등락률
                double changeRate =
                        (prevPrice == 0)
                                ? 0.0
                                : (changePrice / prevPrice) * 100;

                // 상승 / 하락 / 보합
                String change = "EVEN";

                if (changePrice > 0) {
                    change = "RISE";
                } else if (changePrice < 0) {
                    change = "FALL";
                }

                // React로 전달할 DTO 생성
                ExchangeRateResponseDto dto =
                        ExchangeRateResponseDto.builder()
                                .currency(target.itemCode)
                                .name(target.name)
                                .rate(String.format(
                                        "%,.2f",
                                        basePrice
                                ))
                                .change(change)
                                .changePrice(String.format(
                                        "%+.2f",
                                        changePrice
                                ))
                                .changeRate(String.format(
                                        "%+.2f%%",
                                        changeRate
                                ))
                                .build();

                newRates.add(dto);

                log.info(
                        "환율 처리 완료 - {} : {}",
                        target.name,
                        basePrice
                );
            }

            // 모든 통화 데이터를 한 번에 메모리 캐시에 교체
            cachedRates.set(newRates);

            log.info(
                    "⏰ [Java 메모리 캐시] 한국은행 ECOS 환율 정보 갱신 완료 (총 {}개 통화)",
                    newRates.size()
            );

        } catch (Exception e) {

            log.error(
                    "한국은행 ECOS 환율 메모리 캐시 갱신 중 오류 발생",
                    e
            );
        }
    }

    /**
     * 환율 조회 대상
     */
    private static class CurrencyTarget {

        String name;
        String statCode;
        String itemCode;

        public CurrencyTarget(
                String name,
                String statCode,
                String itemCode
        ) {
            this.name = name;
            this.statCode = statCode;
            this.itemCode = itemCode;
        }
    }
}