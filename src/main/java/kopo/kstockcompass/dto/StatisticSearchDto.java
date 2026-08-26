package kopo.kstockcompass.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record StatisticSearchDto(

        @JsonProperty("list_total_count")
        int listTotalCount,

        @JsonProperty("row")
        List<ExchangeRateRowDto> row

) {
}