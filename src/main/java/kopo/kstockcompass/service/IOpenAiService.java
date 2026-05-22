package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.FinancialDTO;

public interface IOpenAiService {
    // 재무데이터 기반 AI 신호등 분석
    AiAnalysisResult analyze(String stockName, FinancialDTO fin);

    record AiAnalysisResult(int score, String summary) {}
}