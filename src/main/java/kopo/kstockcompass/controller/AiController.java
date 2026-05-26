package kopo.kstockcompass.controller;

import kopo.kstockcompass.dto.FinancialDTO;
import kopo.kstockcompass.service.IFinancialService;
import kopo.kstockcompass.service.IOpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiController {

    private final IOpenAiService openAiService;
    private final IFinancialService financialService;

    @GetMapping("/signal/{stockCode}")
    public ResponseEntity<Map<String, Object>> getSignal(
            @PathVariable String stockCode,
            @RequestParam String stockName) {
        try {
            FinancialDTO fin = financialService.getFinancialData(stockCode);
            if (fin == null) {
                return ResponseEntity.ok(Map.of(
                        "score", 3,
                        "summary", "재무 데이터를 불러올 수 없어 분석이 어렵습니다."
                ));
            }

            IOpenAiService.AiAnalysisResult result = openAiService.analyze(stockName, fin);
            return ResponseEntity.ok(Map.of(
                    "score", result.score(),
                    "summary", result.summary()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "score", 3,
                    "summary", "AI 분석 중 오류가 발생했습니다."
            ));
        }
    }

    @PostMapping("/chat/{stockCode}")
    public ResponseEntity<Map<String, Object>> chat(
            @PathVariable String stockCode,
            @RequestParam String stockName,
            @RequestBody Map<String, String> body) {
        try {
            String userMessage = body.get("message");
            FinancialDTO fin = financialService.getFinancialData(stockCode);
            String reply = openAiService.chat(stockName, fin, userMessage);
            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("reply", "죄송합니다. 답변을 생성하는 중 오류가 발생했습니다."));
        }
    }
}