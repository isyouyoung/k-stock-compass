package kopo.kstockcompass.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import kopo.kstockcompass.dto.WeatherResponseDto;
import kopo.kstockcompass.service.IWeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService implements IWeatherService {

    private final WebClient webClient;

    @Value("${weather.api.service-key}")
    private String serviceKey;

    @Value("${weather.api.url}")
    private String weatherApiUrl;

    @Override
    public WeatherResponseDto getRealtimeWeatherAndMarketComment() {
        try {
            // 1. 초단기실황 기준시간 계산 (40분 기준)
            String[] baseDateTime = getBaseDateTime(LocalDateTime.now());
            JsonNode response = fetchWeatherData(baseDateTime[0], baseDateTime[1]);

            String resultCode = response.path("response").path("header").path("resultCode").asText();

            // 2. 기상청 업로드 지연 등으로 NO_DATA(03) 발생 시 1시간 전 데이터로 1회 재요청 (Fallback)
            if ("03".equals(resultCode)) {
                log.warn("기상청 API NO_DATA(03) 발생 - 1시간 전 데이터로 재요청합니다. (baseDate: {}, baseTime: {})", baseDateTime[0], baseDateTime[1]);
                String[] fallbackDateTime = getBaseDateTime(LocalDateTime.now().minusHours(1));
                response = fetchWeatherData(fallbackDateTime[0], fallbackDateTime[1]);
                resultCode = response.path("response").path("header").path("resultCode").asText();
            }

            if (!"00".equals(resultCode)) {
                String resultMsg = response.path("response").path("header").path("resultMsg").asText();
                log.warn("기상청 API 응답 이상 - resultCode: {}, resultMsg: {}", resultCode, resultMsg);
                throw new IllegalStateException("기상청 API 비정상 응답 (resultCode: " + resultCode + ")");
            }

            JsonNode items = response.path("response").path("body").path("items").path("item");

            String temp = "20.0℃";
            String condition = "맑음";

            if (items.isArray()) {
                for (JsonNode item : items) {
                    String category = item.path("category").asText();
                    if ("T1H".equals(category)) {
                        temp = item.path("obsrValue").asText() + "℃";
                    } else if ("PTY".equals(category)) {
                        int ptyCode = item.path("obsrValue").asInt(0);
                        condition = parsePtyCode(ptyCode);
                    }
                }
            }

            String marketComment = generateMarketComment(condition);

            return WeatherResponseDto.builder()
                    .location("서울")
                    .temperature(temp)
                    .weatherCondition(condition)
                    .marketComment(marketComment)
                    .build();

        } catch (Exception e) {
            log.error("공공데이터포털 날씨 API 호출 중 예외 발생: {}", e.getMessage());

            return WeatherResponseDto.builder()
                    .location("서울")
                    .temperature("20.0℃")
                    .weatherCondition("정보 없음")
                    .marketComment("외부 날씨 정보를 불러오지 못했습니다. 재무 신호등을 확인하여 안전한 투자를 이어가세요.")
                    .build();
        }
    }

    private JsonNode fetchWeatherData(String baseDate, String baseTime) {
        URI uri = UriComponentsBuilder.fromHttpUrl(weatherApiUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", "1")
                .queryParam("numOfRows", "10")
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", "60")
                .queryParam("ny", "127")
                .build()
                .encode()
                .toUri();

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(3))
                .block();
    }

    private String[] getBaseDateTime(LocalDateTime targetTime) {
        // 초단기실황은 매시 40분 이후 해당 시각 데이터가 제공됨
        if (targetTime.getMinute() < 40) {
            targetTime = targetTime.minusHours(1);
        }

        String baseDate = targetTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = targetTime.format(DateTimeFormatter.ofPattern("HH00"));

        return new String[]{baseDate, baseTime};
    }

    private String parsePtyCode(int ptyCode) {
        return switch (ptyCode) {
            case 1, 4 -> "비";
            case 2, 3 -> "눈";
            default -> "맑음";
        };
    }

    private String generateMarketComment(String condition) {
        return switch (condition) {
            case "맑음" -> "화창한 날씨처럼 시장 심리가 개선되고 있습니다. 성장주를 유심히 관찰해 보세요.";
            case "비" -> "궂은 날씨로 관망세가 짙을 수 있습니다. 변동성에 대비해 안전한 방어주를 점검해 보세요.";
            case "눈" -> "계절적 요인으로 인한 관련주 변동에 유의하세요.";
            default -> "차트보다 재무제표! 안전한 종목 위주로 투자 전략을 점검하세요.";
        };
    }
}