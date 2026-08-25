package kopo.kstockcompass.dto;

import lombok.Builder;
import java.math.BigDecimal;
import java.util.Map;

@Builder
public record ScenarioRequestDto(
        Map<String, BigDecimal> fixedRates   // key: stockCode, value: 고정 상승률(%)
) {
}