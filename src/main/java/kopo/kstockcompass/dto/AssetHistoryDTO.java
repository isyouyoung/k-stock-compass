package kopo.kstockcompass.dto;

public record AssetHistoryDTO(
        Long histId,
        String userEmail,
        Long totalAsset,
        String regDt
) {}