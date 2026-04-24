package kopo.kstockcompass.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StockSearchDTO {

    // 종목코드 (예: 005930)
    // 역할: 주식 시세 조회 시 사용하는 고유 식별자입니다.
    private String stockCd;

    // 종목명 (예: 삼성전자)
    // 역할: 사용자에게 보여줄 종목 이름입니다.
    private String stockNm;
}