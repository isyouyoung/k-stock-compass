package kopo.kstockcompass.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MarketIndexDTO {
    private String idxNm;   // 지수명 (코스피/코스닥)
    private String clpr;    // 종가
    private String vs;      // 전일 대비
    private String fltRt;   // 등락률
    private String mkp;     // 시가
    private String hipr;    // 고가
    private String lopr;    // 저가
}