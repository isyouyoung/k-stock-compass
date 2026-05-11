package kopo.kstockcompass.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteDTO {

    private Long favId;
    private String userEmail;
    private String stockCd;
    private String stockNm;   // STOCK 테이블에서 조인해서 가져올 종목명
    private String addDt;
}