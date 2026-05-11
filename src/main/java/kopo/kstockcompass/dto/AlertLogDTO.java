package kopo.kstockcompass.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertLogDTO {

    private Long logId;
    private Long alertId;
    private String stockCd;
    private String stockNm;
    private String msg;
    private String isRead;
    private String sendDt;
}