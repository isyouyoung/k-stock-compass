package kopo.kstockcompass.dto;

import java.math.BigDecimal;

public record PortfolioDTO(
        Long portId,
        String userEmail,
        String stockCd,
        String stockNm,
        BigDecimal avgPrice,
        Long quantity,
        String regDt,
        Long currentPrice,
        BigDecimal evalAmt,
        BigDecimal profitAmt,
        BigDecimal profitRate
) {}