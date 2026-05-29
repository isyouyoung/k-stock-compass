package kopo.kstockcompass.controller;

import kopo.kstockcompass.dto.FinancialDTO;
import kopo.kstockcompass.service.IFinancialService;
import kopo.kstockcompass.service.IOpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * [AI 컨트롤러]
 * 역할:
 * - 주식 재무 데이터를 기반으로 AI 분석 점수 생성
 * - 사용자 질문 기반 AI 채팅 응답 처리
 *
 * 전체 흐름:
 * Controller → Service → (DART + Gemini API)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiController {

    private final IOpenAiService openAiService;
    private final IFinancialService financialService;

    /**
     * [AI 신호등 점수 API]
     *
     * 기능:
     * - 특정 종목의 재무 데이터를 기반으로 AI 점수(1~5점) + 요약 생성
     *
     * 흐름:
     * 1. stockCode로 재무 데이터 조회 (DART API)
     * 2. 데이터 없으면 기본 fallback 점수 반환
     * 3. 데이터 있으면 Gemini AI 분석 요청
     * 4. score + summary 반환
     *
     * 특징:
     * - 프론트 UI에서 "투자 신호등"으로 사용됨
     * - 장애 발생 시에도 항상 3점 fallback 유지
     */
    @GetMapping("/signal/{stockCode}")
    public ResponseEntity<Map<String, Object>> getSignal(
            @PathVariable String stockCode,
            @RequestParam String stockName) {

        try {
            FinancialDTO fin = financialService.getFinancialData(stockCode);

            // 재무 데이터가 없는 경우 AI 호출 없이 기본 응답 반환
            if (fin == null) {
                return ResponseEntity.ok(Map.of(
                        "score", 3,
                        "summary", "재무 데이터를 불러올 수 없어 분석이 어렵습니다."
                ));
            }

            // AI 분석 수행 (Gemini API 호출)
            IOpenAiService.AiAnalysisResult result =
                    openAiService.analyze(stockName, fin);

            return ResponseEntity.ok(Map.of(
                    "score", result.score(),
                    "summary", result.summary()
            ));

        } catch (Exception e) {

            // AI 장애 또는 API 실패 시 fallback 응답
            return ResponseEntity.ok(Map.of(
                    "score", 3,
                    "summary", "AI 분석 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * [AI 주식 챗봇 API]
     *
     * 기능:
     * - 사용자가 특정 종목에 대해 질문하면 AI가 자연어로 답변
     *
     * 흐름:
     * 1. stockCode 기반 재무 데이터 조회
     * 2. 사용자 질문(message) 수신
     * 3. Gemini AI로 프롬프트 전달
     * 4. 자연어 답변 생성 후 반환
     *
     * 특징:
     * - 재무 데이터 + AI 지식 혼합 응답 구조
     * - 투자 상담형 챗봇 기능
     */
    @PostMapping("/chat/{stockCode}")
    public ResponseEntity<Map<String, Object>> chat(
            @PathVariable String stockCode,
            @RequestParam String stockName,
            @RequestBody Map<String, String> body) {

        try {
            String userMessage = body.get("message");

            // 재무 데이터 조회
            FinancialDTO fin = financialService.getFinancialData(stockCode);

            // AI 응답 생성
            String reply = openAiService.chat(stockName, fin, userMessage);

            return ResponseEntity.ok(Map.of("reply", reply));

        } catch (Exception e) {

            // 오류 발생 시 fallback 메시지
            return ResponseEntity.ok(Map.of(
                    "reply", "죄송합니다. 답변을 생성하는 중 오류가 발생했습니다."
            ));
        }
    }
}