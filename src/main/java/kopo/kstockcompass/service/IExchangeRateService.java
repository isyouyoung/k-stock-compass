package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.ExchangeRateResponseDto;

import java.util.List;

public interface IExchangeRateService {

    List<ExchangeRateResponseDto> getExchangeRates();
}

// "환율 목록을 가져오는 기능이 필요하다"