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

/**
 * [Gemini AI 분석 서비스]
 * 역할:
 * - 기업 재무 데이터(FinancialDTO)를 기반으로 AI 분석 수행
 * - 재무 안정성 점수(1~5점) 생성
 * - 자연어 기반 기업 질의응답(chat) 제공
 *
 * 사용 API:
 * - Google Gemini (Generative Language API)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService implements IOpenAiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * [재무 분석 AI]
     * - 재무 데이터를 프롬프트로 구성
     * - Gemini에게 점수 + 요약 생성 요청
     * - JSON 형태로 결과 파싱
     */
    @Override
    public AiAnalysisResult analyze(String stockName, FinancialDTO fin) {
        try {
            // 1. AI에게 전달할 프롬프트 생성
            String prompt = buildPrompt(stockName, fin);

            // 2. Gemini API 요청 body 구성 (contents > parts 구조)
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            // 3. Gemini API 호출
            String response = webClient.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()

                    // 4. API 에러 발생 시 응답 바디 로그 출력
                    .onStatus(status -> status.isError(), clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                                log.error("🔴 구글 API 에러 응답 바디: {}", errorBody);
                                return Mono.error(new RuntimeException("Google API Error: " + errorBody));
                            })
                    )
                    .bodyToMono(String.class)
                    .block();

            // 5. Gemini 응답 JSON 파싱
            JsonNode root = objectMapper.readTree(response);

            // 6. 실제 AI 텍스트 추출
            String content = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            // 7. markdown 코드 블럭 제거 후 JSON 변환
            content = content.replaceAll("```json", "").replaceAll("```", "").trim();
            JsonNode result = objectMapper.readTree(content);

            // 8. 결과 값 파싱 (기본값 포함)
            int score = result.path("score").asInt(3);
            String summary = result.path("summary").asText("분석 결과를 불러올 수 없습니다.");

            log.info("AI 신호등 분석 완료: {} → {}점", stockName, score);

            return new AiAnalysisResult(score, summary);

        } catch (Exception e) {
            // AI 호출 전체 실패 시 fallback 처리
            log.error("AI 분석 중 진짜 에러 발생! 원인: ", e);

            return new AiAnalysisResult(
                    3,
                    "현재 AI 분석 요청이 많아 기본 분석 결과를 표시합니다. 잠시 후 다시 시도해주세요."
            );
        }
    }

    /**
     * [프롬프트 생성 로직]
     * - 재무 데이터를 사람이 이해 가능한 형태로 변환
     * - AI가 1~5점 평가하도록 규칙 제공
     */
    private String buildPrompt(String stockName, FinancialDTO fin) {

        String debtStr = fin.debtRatio() == null ? "N/A" :
                fin.debtRatio().compareTo(java.math.BigDecimal.valueOf(-1)) == 0 ? "자본잠식" :
                        fin.debtRatio() + "%";

        String marginStr = fin.operatingMargin() == null ? "N/A" : fin.operatingMargin() + "%";
        String currentStr = fin.currentRatio() == null ? "N/A" : fin.currentRatio() + "%";

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
                stockName, fin.bsnsYear(), debtStr, marginStr, currentStr);
    }

    /**
     * [AI 채팅 기능]
     * - 사용자 질문 + 재무데이터 기반 자연어 응답 생성
     * - 분석 리포트보다 자유로운 Q&A 형태
     */
    @Override
    public String chat(String stockName, FinancialDTO fin, String userMessage) {
        try {
            String debtStr = fin != null && fin.debtRatio() != null ? fin.debtRatio() + "%" : "N/A";
            String marginStr = fin != null && fin.operatingMargin() != null ? fin.operatingMargin() + "%" : "N/A";
            String currentStr = fin != null && fin.currentRatio() != null ? fin.currentRatio() + "%" : "N/A";
            String bsnsYear = fin != null ? fin.bsnsYear() : "N/A";

            // 1. AI 채팅용 프롬프트 생성
            String prompt = String.format("""
                당신은 %s의 전문 재무/기업 분석 AI 에이전트입니다.
                
                [답변 규칙]
                1. 제공된 실제 재무 데이터를 최우선으로 사용하세요.
                2. 사용자가 기업의 제품, 사업, 공장, 기술력 등을 질문하면,
                   당신이 알고 있는 일반적인 기업 정보를 활용해 답변하세요.
                3. 재무 데이터에 없는 내용은 '일반적으로 알려진 정보 기준'이라고 자연스럽게 설명하세요.
                4. 모르는 내용은 추측하지 말고 솔직하게 답하세요.
                5. 답변은 너무 딱딱하지 않게, 실제 애널리스트처럼 자연스럽게 설명하세요.
                6. 답변 길이는 3~6문장 정도로 간결하게 작성하세요.
                
                [실제 재무 데이터]
                - 기업명: %s
                - 사업연도: %s년
                - 부채비율: %s
                - 영업이익률: %s
                - 유동비율: %s
                
                사용자 질문:
                %s
                """,
                    stockName, stockName, bsnsYear, debtStr, marginStr, currentStr, userMessage);

            // 2. 요청 body 구성
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            // 3. Gemini API 호출
            String response = webClient.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()

                    // 4. API 오류 처리
                    .onStatus(status -> status.isError(), clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                                log.error("Gemini 채팅 에러: {}", errorBody);
                                return Mono.error(new RuntimeException("Gemini Error: " + errorBody));
                            })
                    )
                    .bodyToMono(String.class)
                    .block();

            // 5. 응답 파싱
            JsonNode root = objectMapper.readTree(response);

            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText("답변을 생성할 수 없습니다.");

        } catch (Exception e) {
            // AI 채팅 실패 fallback
            log.error("채팅 실패: {}", e.getMessage());
            return "죄송합니다. 현재 AI 응답이 어렵습니다. 잠시 후 다시 시도해주세요.";
        }
    }
}