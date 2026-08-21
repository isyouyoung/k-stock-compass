package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.WeatherResponseDto;

public interface IWeatherService {

    /**
     * 실시간 날씨 정보를 조회하고, 날씨 조건에 따른 증시 한줄평(marketComment)을 가공하여 반환
     */
    WeatherResponseDto getRealtimeWeatherAndMarketComment();
}