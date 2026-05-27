package kopo.kstockcompass.dto;

import java.math.BigDecimal;

public record FinancialDTO(
        String stockCd,
        String corpName,
        String bsnsYear,
        BigDecimal currentAsset,
        BigDecimal currentLiab,
        BigDecimal totalLiab,
        BigDecimal totalEquity,
        BigDecimal revenue,
        BigDecimal operatingProfit,
        BigDecimal netIncome,
        BigDecimal debtRatio,
        BigDecimal operatingMargin,
        BigDecimal currentRatio
) {}