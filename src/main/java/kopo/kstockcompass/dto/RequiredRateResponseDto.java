package kopo.kstockcompass.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record RequiredRateResponseDto(
        BigDecimal targetProfitRate,          // 설정된 목표 수익률
        BigDecimal totalCurrentValue,         // 전체 현재평가금액
        BigDecimal totalTargetValue,          // 목표 평가금액
        BigDecimal totalRequiredProfit,       // 목표 달성을 위해 필요한 총 수익금
        List<StockRequiredRate> stockRates    // 종목별 필요 상승률
) {
    @Builder
    public record StockRequiredRate(
            String stockCode,
            String stockName,
            BigDecimal currentPrice,
            BigDecimal currentValue,
            BigDecimal requiredRate            // 이 종목 하나가 전체 목표 수익을 만든다고 가정한 필요 상승률
    ) {
    }
}