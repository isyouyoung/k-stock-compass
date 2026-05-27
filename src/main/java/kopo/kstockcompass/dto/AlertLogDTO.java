package kopo.kstockcompass.dto;

public record AlertLogDTO(
        Long logId,
        Long alertId,
        String stockCd,
        String stockNm,
        String msg,
        String isRead,
        String sendDt
) {}