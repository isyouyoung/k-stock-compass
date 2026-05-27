package kopo.kstockcompass.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StockItemDTO(
        @JsonProperty("srtnCd") String srtnCd,
        @JsonProperty("itmsNm") String itmsNm,
        @JsonProperty("clpr") String clpr,
        @JsonProperty("fltRt") String fltRt,
        @JsonProperty("vs") String vs,
        @JsonProperty("mrktCtg") String mrktCtg,
        @JsonProperty("oprc") String oprc,
        @JsonProperty("hgpr") String hgpr,
        @JsonProperty("lwpr") String lwpr,
        @JsonProperty("acmlVol") String acmlVol,
        @JsonProperty("htsMktcap") String htsMktcap,
        @JsonProperty("w52Hgpr") String w52Hgpr
) {}