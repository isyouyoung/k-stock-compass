package kopo.kstockcompass.dto;

import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record InvestmentGoalRequestDto(
        BigDecimal targetProfitRate
) {
}