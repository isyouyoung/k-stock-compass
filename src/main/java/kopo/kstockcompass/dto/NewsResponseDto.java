package kopo.kstockcompass.dto;

import lombok.Builder;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * [네이버 뉴스 응답 DTO]
 * - 네이버 API의 JSON 응답 중 필요한 필드만 추출하여 프론트엔드에 전달
 */
@Builder
public record NewsResponseDto(
        String title,          // 뉴스 제목 (HTML 태그 제거)
        String originallink,   // 언론사 원본 기사 URL
        String description,    // 뉴스 본문 요약 (HTML 태그 제거)
        String pubDate         // 기사 발행 일시 (yyyy년 M월 d일 형식)
) {
    // 네이버 pubDate 원본 포맷: "Mon, 24 Aug 2026 09:44:00 +0900" (RFC 1123)
    private static final DateTimeFormatter NAVER_DATE_FORMAT = DateTimeFormatter.RFC_1123_DATE_TIME;
    private static final DateTimeFormatter KOREAN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREAN);

    // 네이버 원본 응답 -> 우리 서비스 응답 DTO로 변환
    public static NewsResponseDto from(NaverNewsApiResponseDto.NewsItemDto item) {
        return NewsResponseDto.builder()
                .title(removeHtmlTags(item.title()))
                .originallink(item.originallink())
                .description(removeHtmlTags(item.description()))
                .pubDate(formatPubDate(item.pubDate()))
                .build();
    }

    // <b>, </b> 등 HTML 태그 제거 + HTML 엔티티 복원
    private static String removeHtmlTags(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]*>", "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&");
    }

    // 영문 RFC 1123 날짜 -> 한글 "yyyy년 M월 d일" 형식으로 변환
    private static String formatPubDate(String rawPubDate) {
        if (rawPubDate == null || rawPubDate.isBlank()) return "";
        try {
            ZonedDateTime dateTime = ZonedDateTime.parse(rawPubDate, NAVER_DATE_FORMAT);
            return dateTime.format(KOREAN_DATE_FORMAT);
        } catch (Exception e) {
            // 파싱 실패 시 원본 그대로 반환 (방어 처리)
            return rawPubDate;
        }
    }
}