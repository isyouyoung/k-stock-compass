package kopo.kstockcompass.dto;

import lombok.Builder;
import java.math.BigDecimal;
import java.util.List;

@Builder
public record RequiredRateResponseDto(
        BigDecimal targetProfitRate,          // 설정된 목표 수익률
        BigDecimal totalCurrentValue,          // 전체 현재평가금액
        BigDecimal totalTargetValue,           // 전체 목표금액
        BigDecimal overallRequiredRate,        // 전체 계좌 균등 필요 상승률
        List<StockRequiredRate> stockRates     // 종목별 필요 상승률
) {
    @Builder
    public record StockRequiredRate(
            String stockCode,
            String stockName,
            BigDecimal currentPrice,
            BigDecimal currentValue,
            BigDecimal requiredRate            // 이 종목만 봤을 때 필요 상승률
    ) {
    }
}