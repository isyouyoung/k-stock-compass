package kopo.kstockcompass.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {
    private String userEmail;
    private BigDecimal cash;
    private BigDecimal loan;
}