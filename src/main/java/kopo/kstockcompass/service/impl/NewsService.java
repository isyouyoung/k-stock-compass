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

    // WebClient를 이용하여 네이버 뉴스 Open API와 통신
    private final WebClient webClient;

    // application.properties에 저장된 네이버 API 인증 정보 주입
    @Value("${naver.openapi.client-id}")
    private String clientId;

    @Value("${naver.openapi.client-secret}")
    private String clientSecret;

    // 네이버 뉴스 검색 Open API 주소
    private static final String NAVER_NEWS_API_URL =
            "https://openapi.naver.com/v1/search/news.json";

    @Override
    public List<NewsResponseDto> searchStockNews(String query) {
        try {
            // 1. 네이버 뉴스 API 요청 URI 생성
            // 검색어, 조회 개수, 시작 위치, 최신순 정렬 조건을 쿼리 파라미터로 설정
            URI uri = UriComponentsBuilder.fromHttpUrl(NAVER_NEWS_API_URL)
                    .queryParam("query", query)
                    .queryParam("display", 10)
                    .queryParam("start", 1)
                    .queryParam("sort", "date")
                    .build()
                    .encode()
                    .toUri();

            // 2. WebClient를 이용하여 네이버 뉴스 API 호출
            // API 인증 정보는 URL이 아닌 HTTP 요청 헤더에 전달
            // 네이버의 JSON 응답을 NaverNewsApiResponseDto 객체로 변환
            NaverNewsApiResponseDto response = webClient.get()
                    .uri(uri)
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .retrieve()
                    .bodyToMono(NaverNewsApiResponseDto.class)
                    .block();

            // 3. Optional을 활용한 null 안전 처리
            // response 또는 response.items()가 null인 경우 빈 리스트를 반환
            // 기존 if문을 사용한 null 검사 대신 Optional 체이닝으로 처리
            return Optional.ofNullable(response)
                    .map(NaverNewsApiResponseDto::items)
                    .orElseGet(() -> {
                        // 외부 API 응답이 없거나 뉴스 목록이 null인 경우 로그 기록
                        log.warn("네이버 뉴스 API 응답이 비어있음 - query: {}", query);
                        return List.of();
                    })
                    .stream()
                    // 4. 네이버 API 원본 DTO를 프론트엔드 응답용 DTO로 변환
                    // NewsResponseDto.from()에서 HTML 태그 및 특수문자 등을 정제
                    .map(NewsResponseDto::from)
                    .toList();

        } catch (Exception e) {
            // 5. API 호출 또는 데이터 처리 중 예외 발생 시
            // 서버 오류 대신 빈 리스트를 반환하여 서비스가 중단되지 않도록 처리
            log.error(
                    "네이버 뉴스 API 호출 중 에러 발생 - query: {}, error: {}",
                    query,
                    e.getMessage()
            );
            // Fallback: 실패 시 빈 리스트 반환
            return List.of();
        }
    }
}