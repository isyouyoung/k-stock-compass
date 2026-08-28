package kopo.kstockcompass.service.impl;

import kopo.kstockcompass.dto.NaverNewsApiResponseDto;
import kopo.kstockcompass.dto.NewsResponseDto;
import kopo.kstockcompass.service.INewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService implements INewsService {

    private final WebClient webClient;

    @Value("${naver.openapi.client-id}")
    private String clientId;

    @Value("${naver.openapi.client-secret}")
    private String clientSecret;

    private static final String NAVER_NEWS_API_URL = "https://openapi.naver.com/v1/search/news.json";

    @Override
    public List<NewsResponseDto> searchStockNews(String query) {
        try {
            // 1. 요청 URI 생성 (쿼리 파라미터: 검색어, 개수, 정렬)
            URI uri = UriComponentsBuilder.fromHttpUrl(NAVER_NEWS_API_URL)
                    .queryParam("query", query)
                    .queryParam("display", 10)
                    .queryParam("start", 1)
                    .queryParam("sort", "date") // 최신순 정렬
                    .build()
                    .encode()
                    .toUri();

            // 2. WebClient 호출 (인증 정보를 HTTP 헤더에 실어 동기식 호출)
            NaverNewsApiResponseDto response = webClient.get()
                    .uri(uri)
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .retrieve()
                    .bodyToMono(NaverNewsApiResponseDto.class)
                    .block();

            // 3. Optional을 활용한 응답 방어 처리
            return Optional.ofNullable(response)
                    .map(NaverNewsApiResponseDto::items)
                    .orElseGet(() -> {
                        log.warn("네이버 뉴스 API 응답이 비어있음 - query: {}", query);
                        return List.of();
                    })
                    .stream()
                    .map(NewsResponseDto::from)
                    .toList();

        } catch (Exception e) {
            log.error("네이버 뉴스 API 호출 중 에러 발생 - query: {}, error: {}", query, e.getMessage());
            return List.of(); // Fallback: 실패 시 빈 리스트 반환
        }
    }
}