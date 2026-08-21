package kopo.kstockcompass.dto;

import lombok.Builder;

@Builder
public record WeatherResponseDto(
        String location,          // 지역 (예: 서울)
        String temperature,       // 기온 (예: 24.5℃)
        String weatherCondition,  // 날씨 상태 (예: 맑음, 비 등)
        String marketComment      // 백엔드가 조합한 증시 한줄평 멘트
) {
}