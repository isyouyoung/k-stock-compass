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
public class PortfolioDTO {
    private Long portId;
    private String userEmail;
    private String stockCd;
    private String stockNm;      // DB에 없고 조회 시 채워줌
    private BigDecimal avgPrice;
    private Long quantity;
    private String regDt;

    // 현재가 기반 계산 필드 (서비스에서 채워줌)
    private Long currentPrice;   // 현재가
    private BigDecimal evalAmt;  // 평가금액 (현재가 × 수량)
    private BigDecimal profitAmt; // 손익금액
    private BigDecimal profitRate; // 손익률
}