package kopo.kstockcompass.dto;

import lombok.Builder;

@Builder
public record ExchangeRateResponseDto(
        String currency,      // 통화 코드 (USD, JPY, EUR, CNY)
        String name,          // 통화 이름
        String rate,          // 환율
        String change,        // 전일 대비 (RISE, FALL, EVEN)
        String changePrice,   // 전일 대비 금액
        String changeRate     // 등락률
) {
}