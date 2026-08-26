package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.ExchangeRateResponseDto;

import java.util.List;

public interface IExchangeRateService {

    List<ExchangeRateResponseDto> getExchangeRates();
}

// "환율 목록을 가져오는 기능이 필요하다"
//아직 어떻게 가져오는지는 모름. 구현체에서 구현 예정