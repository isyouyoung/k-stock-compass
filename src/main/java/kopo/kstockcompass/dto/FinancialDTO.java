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
public class FinancialDTO {
    private String stockCd;
    private String corpName;
    private String bsnsYear;      // 사업연도

    // 재무상태표
    private BigDecimal currentAsset;   // 유동자산
    private BigDecimal currentLiab;    // 유동부채
    private BigDecimal totalLiab;      // 부채총계
    private BigDecimal totalEquity;    // 자본총계

    // 손익계산서
    private BigDecimal revenue;        // 매출액
    private BigDecimal operatingProfit; // 영업이익
    private BigDecimal netIncome;      // 당기순이익

    // 계산 지표
    private BigDecimal debtRatio;      // 부채비율
    private BigDecimal operatingMargin; // 영업이익률
    private BigDecimal currentRatio;   // 유동비율
}