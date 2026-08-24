package kopo.kstockcompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * [네이버 뉴스 API 원본 응답 DTO]
 * - 네이버가 실제로 주는 JSON 구조 그대로 매핑 (파싱 전용)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverNewsApiResponseDto(
        int total,
        List<NewsItemDto> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NewsItemDto(
            String title,
            String originallink,
            String link,
            String description,
            String pubDate
    ) {}
}