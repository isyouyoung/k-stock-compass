package kopo.kstockcompass.dto;

import lombok.Builder;

/**
 * [네이버 뉴스 응답 DTO]
 * - 네이버 API의 JSON 응답 중 필요한 필드만 추출하여 프론트엔드에 전달
 */
@Builder
public record NewsResponseDto(
        String title,          // 뉴스 제목 (HTML 태그 제거)
        String originallink,   // 언론사 원본 기사 URL
        String description,    // 뉴스 본문 요약 (HTML 태그 제거)
        String pubDate         // 기사 발행 일시
) {
    // 네이버 원본 응답 -> 우리 서비스 응답 DTO로 변환
    public static NewsResponseDto from(NaverNewsApiResponseDto.NewsItemDto item) {
        return NewsResponseDto.builder()
                .title(removeHtmlTags(item.title()))
                .originallink(item.originallink())
                .description(removeHtmlTags(item.description()))
                .pubDate(item.pubDate())
                .build();
    }

    // <b>, </b> 등 HTML 태그 제거 + HTML 엔티티 복원
    private static String removeHtmlTags(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]*>", "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&");
    }
}