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
public class AlertDTO {

    private Long alertId;
    private String userEmail;
    private String stockCd;
    private String stockNm;
    private BigDecimal targetPrice;
    private String regDt;
    private String direction;
}