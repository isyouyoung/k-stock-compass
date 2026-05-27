package kopo.kstockcompass.dto;

import java.math.BigDecimal;

public record SimulatorDTO(
        Long simId,
        String userEmail,
        String stockCd,
        String stockNm,
        BigDecimal avgPrice,
        Long quantity,
        BigDecimal targetPrice,
        String regDt,
        BigDecimal expectedRevenue,
        BigDecimal investAmt,
        BigDecimal expectedProfit,
        BigDecimal expectedProfitRate
) {}