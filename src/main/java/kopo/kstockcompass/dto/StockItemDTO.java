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
    private String mrktCtg;  // 시장구분 (코스피/코스닥)

    private String oprc;     // 시가
    private String hgpr;     // 고가
    private String lwpr;     // 저가
    private String acmlVol;  // 거래량
    private String htsMktcap; // 시가총액
    private String w52Hgpr;  // 52주 최고
}