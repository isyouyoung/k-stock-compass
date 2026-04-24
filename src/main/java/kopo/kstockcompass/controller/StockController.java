package kopo.kstockcompass.controller;

import kopo.kstockcompass.dto.StockItemDTO;
import kopo.kstockcompass.dto.StockSearchDTO;
import kopo.kstockcompass.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stockService;

    /**
     * 주식 시세 조회 API
     * 역할: 종목코드 + 기준일자로 해당 종목의 시세를 조회합니다.
     * 사용: GET /api/stock/price?stockCode=005930&baseDate=20260422
     */
    @GetMapping("/price")
    public ResponseEntity<StockItemDTO> getStockPrice(
            @RequestParam String stockCode,
            @RequestParam String baseDate) {
        StockItemDTO result = stockService.getStockPrice(stockCode, baseDate);
        return ResponseEntity.ok(result);
    }

    /**
     * 전체 종목 초기화 및 수집 API
     * 역할: 기존 종목 데이터를 삭제하고 공공데이터 API에서 전체 종목을 새로 저장합니다.
     * 사용: 서버 최초 실행 시 또는 종목 데이터 갱신이 필요할 때 한 번만 호출합니다.
     */
    @PostMapping("/init")
    public ResponseEntity<String> initStocks() {
        stockService.initStocks();
        return ResponseEntity.ok("종목 초기화 및 수집이 완료되었습니다.");
    }

    /**
     * 종목 검색 API
     * 역할: 키워드로 종목명을 검색합니다.
     * 사용: GET /api/stock/search?keyword=삼성
     */
    @GetMapping("/search")
    public ResponseEntity<List<StockSearchDTO>> searchStocks(@RequestParam String keyword) {
        return ResponseEntity.ok(stockService.searchStocks(keyword));
    }
}