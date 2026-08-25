package kopo.kstockcompass.dto;

import kopo.kstockcompass.repository.entity.TradeLogEntity;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter; // 💡 포맷터 import 추가

@Builder
public record TradeLogResponseDto(
        Long id,
        String stockCode,
        String stockName,
        BigDecimal buyPrice,
        Integer quantity,
        String createdAt // 💡 프론트엔드를 위해 String으로 변경!
) {
    public static TradeLogResponseDto from(TradeLogEntity entity) {

        // 💡 게시판, 목표수익률과 똑같은 포맷 양식 적용
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return TradeLogResponseDto.builder()
                .id(entity.getId())
                .stockCode(entity.getStockCode())
                .stockName(entity.getStockName())
                .buyPrice(entity.getBuyPrice())
                .quantity(entity.getQuantity())
                .createdAt(
                        entity.getCreatedAt() != null
                                ? entity.getCreatedAt().format(formatter)
                                : ""
                )
                .build();
    }
}