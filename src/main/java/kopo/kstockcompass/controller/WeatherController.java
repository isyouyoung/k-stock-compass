package kopo.kstockcompass.controller;

import kopo.kstockcompass.dto.WeatherResponseDto;
import kopo.kstockcompass.service.IWeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final IWeatherService weatherService;

    @GetMapping
    public ResponseEntity<WeatherResponseDto> getWeatherAndMarketComment() {
        WeatherResponseDto response = weatherService.getRealtimeWeatherAndMarketComment();
        return ResponseEntity.ok(response);
    }
}