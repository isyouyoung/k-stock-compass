package kopo.kstockcompass.dto;

import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record TradeLogRequestDto(
        String stockCode,
        String stockName,
        BigDecimal buyPrice,
        Integer quantity
) {
}