package kopo.kstockcompass.dto;

public record MarketIndexDTO(
        String idxNm,
        String clpr,
        String vs,
        String fltRt,
        String mkp,
        String hipr,
        String lopr
) {}