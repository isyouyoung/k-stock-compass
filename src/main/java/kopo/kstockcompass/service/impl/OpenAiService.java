package kopo.kstockcompass.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.kstockcompass.dto.FinancialDTO;
import kopo.kstockcompass.service.IOpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService implements IOpenAiService {

    @Value("${openai.api.key}")
    private String openAiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Override
    public AiAnalysisResult analyze(String stockName, FinancialDTO fin) {
        try {
            String prompt = buildPrompt(stockName, fin);

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4o-mini",
                    "max_tokens", 500,
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "당신은 한국 주식 재무 분석 전문가입니다. 재무 데이터를 분석하여 JSON 형식으로만 응답하세요."),
                            Map.of("role", "user", "content", prompt)
                    )
            );

            String response = webClient.post()
                    .uri("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + openAiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            // JSON 파싱
            content = content.replaceAll("```json", "").replaceAll("```", "").trim();
            JsonNode result = objectMapper.readTree(content);

            int score = result.path("score").asInt(3);
            String summary = result.path("summary").asText("분석 결과를 불러올 수 없습니다.");

            log.info("AI 신호등 분석 완료: {} → {}점", stockName, score);
            return new AiAnalysisResult(score, summary);

        } catch (Exception e) {
            log.error("AI 분석 실패: {}", e.getMessage());
            return new AiAnalysisResult(3, "AI 분석 중 오류가 발생했습니다.");
        }
    }

    private String buildPrompt(String stockName, FinancialDTO fin) {
        String debtStr = fin.getDebtRatio() == null ? "N/A" :
                fin.getDebtRatio().compareTo(java.math.BigDecimal.valueOf(-1)) == 0 ? "자본잠식" :
                        fin.getDebtRatio() + "%";
        String marginStr = fin.getOperatingMargin() == null ? "N/A" : fin.getOperatingMargin() + "%";
        String currentStr = fin.getCurrentRatio() == null ? "N/A" : fin.getCurrentRatio() + "%";

        return String.format("""
                다음은 %s의 %s년 재무 데이터입니다:
                - 부채비율: %s (낮을수록 안전, 100%% 이하 양호)
                - 영업이익률: %s (높을수록 좋음, 10%% 이상 양호)
                - 유동비율: %s (높을수록 안전, 100%% 이상 양호)
                
                위 데이터를 종합하여 재무 안정성을 1~5점으로 평가하고 한국어로 분석해주세요.
                점수 기준:
                1점: 매우위험 (부채비율 200%% 초과, 영업이익률 0%% 미만)
                2점: 위험 (부채비율 150%% 초과 또는 영업이익률 5%% 미만)
                3점: 보통 (부채비율 100~150%%, 영업이익률 5~10%%)
                4점: 양호 (부채비율 100%% 이하, 영업이익률 10%% 이상)
                5점: 매우안전 (부채비율 50%% 이하, 영업이익률 20%% 이상, 유동비율 200%% 이상)
                
                반드시 아래 JSON 형식으로만 응답하세요:
                {"score": 점수, "summary": "3~4문장 분석 요약"}
                """,
                stockName, fin.getBsnsYear(), debtStr, marginStr, currentStr);
    }
}