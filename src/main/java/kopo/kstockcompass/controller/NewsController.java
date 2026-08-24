package kopo.kstockcompass.controller;

import kopo.kstockcompass.dto.NewsResponseDto;
import kopo.kstockcompass.service.INewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/news")
public class NewsController {

    private final INewsService newsService; // 구현체가 아닌 인터페이스로 주입 (DI 원칙)

    /**
     * [종목 뉴스 조회 API]
     * 역할: 종목명(또는 검색어)을 받아 네이버 뉴스 검색 결과를 반환
     * 인증 불필요 - 누구나 뉴스를 조회할 수 있음
     * 예: GET /api/news?query=삼성전자
     */
    @GetMapping
    public ResponseEntity<List<NewsResponseDto>> getStockNews(@RequestParam String query) {
        try {
            List<NewsResponseDto> news = newsService.searchStockNews(query);
            return ResponseEntity.ok(news);
        } catch (Exception e) {
            log.error("종목 뉴스 조회 중 에러 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}