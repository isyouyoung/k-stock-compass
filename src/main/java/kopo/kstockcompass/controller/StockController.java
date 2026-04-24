package kopo.kstockcompass.controller;

import kopo.kstockcompass.dto.StockItemDTO;
import kopo.kstockcompass.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stockService;

    // 주식 시세 조회
    @GetMapping("/price")
    public ResponseEntity<StockItemDTO> getStockPrice(
            @RequestParam String stockCode,
            @RequestParam String baseDate) {
        StockItemDTO result = stockService.getStockPrice(stockCode, baseDate);
        return ResponseEntity.ok(result);
    }
}