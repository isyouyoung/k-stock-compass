package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.FinancialDTO;

public interface IOpenAiService {
    // 재무데이터 기반 AI 신호등 분석
    // AI_ANLS 7번 실제 구현체로 가기위해
    // analyze => go to Implementation 클릭
    AiAnalysisResult analyze(String stockCode, String stockName, FinancialDTO fin);

    record AiAnalysisResult(int score, String summary) {}

    String chat(String stockName, FinancialDTO fin, String userMessage);
}