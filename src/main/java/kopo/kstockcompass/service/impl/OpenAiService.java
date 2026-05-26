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
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService implements IOpenAiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Override
    public AiAnalysisResult analyze(String stockName, FinancialDTO fin) {
        try {
            String prompt = buildPrompt(stockName, fin);

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            String response = webClient.post()
                    .uri("https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    // 에러 발생 시 구글이 보낸 응답 바디를 로그로 찍음
                    .onStatus(status -> status.isError(), clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                                log.error("🔴 구글 API 에러 응답 바디: {}", errorBody);
                                return Mono.error(new RuntimeException("Google API Error: " + errorBody));
                            })
                    )
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            String content = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            // JSON 파싱
            content = content.replaceAll("```json", "").replaceAll("```", "").trim();
            JsonNode result = objectMapper.readTree(content);

            int score = result.path("score").asInt(3);
            String summary = result.path("summary").asText("분석 결과를 불러올 수 없습니다.");

            log.info("AI 신호등 분석 완료: {} → {}점", stockName, score);
            return new AiAnalysisResult(score, summary);

        } catch (Exception e) {
            // 이 부분을 아래와 같이 수정하세요!
            log.error("AI 분석 중 진짜 에러 발생! 원인: ", e);

            // 사용자에게 보여줄 메시지도 조금 더 친절하게 바꿀 수 있습니다.
            return new AiAnalysisResult(3, "현재 AI 분석 요청이 많아 기본 분석 결과를 표시합니다. 잠시 후 다시 시도해주세요.");
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

    @Override
    public String chat(String stockName, FinancialDTO fin, String userMessage) {
        try {
            String context = fin != null ? String.format("""
                [%s 재무 정보 (%s년)]
                - 부채비율: %s
                - 영업이익률: %s
                - 유동비율: %s
                - 매출액: %s원
                - 영업이익: %s원
                - 당기순이익: %s원
                """,
                    stockName, fin.getBsnsYear(),
                    fin.getDebtRatio() != null ? fin.getDebtRatio() + "%" : "N/A",
                    fin.getOperatingMargin() != null ? fin.getOperatingMargin() + "%" : "N/A",
                    fin.getCurrentRatio() != null ? fin.getCurrentRatio() + "%" : "N/A",
                    fin.getRevenue() != null ? fin.getRevenue() : "N/A",
                    fin.getOperatingProfit() != null ? fin.getOperatingProfit() : "N/A",
                    fin.getNetIncome() != null ? fin.getNetIncome() : "N/A"
            ) : "";

            String prompt = String.format("""
                당신은 한국 주식 재무 분석 전문가입니다.
                %s
                
                사용자 질문: %s
                
                위 재무 데이터를 바탕으로 친절하고 전문적으로 답변해주세요.
                답변은 3~5문장으로 간결하게 해주세요.
                투자 조언은 제공하지 말고 재무 분석 정보만 제공하세요.
                """, context, userMessage);

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            String response = webClient.post()
                    .uri("https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                                log.error("Gemini 채팅 에러: {}", errorBody);
                                return Mono.error(new RuntimeException("Gemini Error: " + errorBody));
                            })
                    )
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText("답변을 생성할 수 없습니다.");

        } catch (Exception e) {
            log.error("채팅 실패: {}", e.getMessage());
            return "죄송합니다. 현재 AI 응답이 어렵습니다. 잠시 후 다시 시도해주세요.";
        }
    }
}