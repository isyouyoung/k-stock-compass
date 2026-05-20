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
public class SimulatorDTO {
    private Long simId;
    private String userEmail;
    private String stockCd;
    private String stockNm;
    private BigDecimal avgPrice;
    private Long quantity;
    private BigDecimal targetPrice;
    private String regDt;

    // 계산 필드 (서비스에서 채워줌)
    private BigDecimal expectedRevenue;  // 예상 매도금액 (목표가 × 수량)
    private BigDecimal investAmt;        // 투자원금 (평단가 × 수량)
    private BigDecimal expectedProfit;   // 예상 손익
    private BigDecimal expectedProfitRate; // 예상 수익률
}