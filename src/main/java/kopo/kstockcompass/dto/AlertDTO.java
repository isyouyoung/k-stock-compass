package kopo.kstockcompass.dto;

import java.math.BigDecimal;

public record AlertDTO(
        Long alertId,
        String userEmail,
        String stockCd,
        String stockNm,
        BigDecimal targetPrice,
        String regDt,
        String direction
) {}