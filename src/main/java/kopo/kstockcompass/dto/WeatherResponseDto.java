package kopo.kstockcompass.dto;

import lombok.Builder;

/**
 * [날씨 및 증시 멘트 응답 DTO]
 * - 프론트엔드(UI)에 전달할 날씨 정보 및 백엔드에서 조합한 증시 한줄평을 담는 객체
 * - Java 14+ Record 구조를 사용하여 불변성(Immutable) 보장 및 가독성 확보
 */
@Builder
public record WeatherResponseDto(
        String location,          // 사용자 위치 또는 기본 설정 지역 (예: 서울)
        String temperature,       // 현재 기온 (예: 24.5℃)
        String weatherCondition,  // 날씨 상태 (예: 맑음, 비, 눈 등)
        String marketComment      // 날씨 상태와 증시 상황을 연계하여 생성한 한줄평 멘트
) {
}