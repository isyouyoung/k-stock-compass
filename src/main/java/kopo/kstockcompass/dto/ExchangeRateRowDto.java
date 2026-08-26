package kopo.kstockcompass.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record ExchangeRateRowDto(

        @JsonProperty("STAT_CODE")
        String statCode,

        @JsonProperty("STAT_NAME")
        String statName,

        @JsonProperty("ITEM_CODE1")
        String itemCode1,

        @JsonProperty("ITEM_NAME1")
        String itemName1,

        @JsonProperty("UNIT_NAME")
        String unitName,

        @JsonProperty("TIME")
        String time,

        @JsonProperty("DATA_VALUE")
        String dataValue

) {
}