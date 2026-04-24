package kopo.kstockcompass.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockItemDTO {
    private String srtnCd;   // 종목코드
    private String itmsNm;   // 종목명
    private String clpr;     // 종가
    private String fltRt;    // 등락률
    private String vs;       // 전일대비
}