package kopo.kstockcompass.dto;

import java.math.BigDecimal;

public record AccountDTO(
        String userEmail,
        BigDecimal cash,
        BigDecimal loan
) {}