package kopo.kstockcompass.dto;

public record FavoriteDTO(
        Long favId,
        String userEmail,
        String stockCd,
        String stockNm,
        String addDt
) {}