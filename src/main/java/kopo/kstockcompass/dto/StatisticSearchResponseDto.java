package kopo.kstockcompass.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record StatisticSearchResponseDto(

        @JsonProperty("StatisticSearch")
        StatisticSearchDto statisticSearch

) {
}