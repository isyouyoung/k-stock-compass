package kopo.kstockcompass.controller;

import kopo.kstockcompass.dto.ExchangeRateResponseDto;
import kopo.kstockcompass.service.IExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exchange-rate")
public class ExchangeRateController {

    private final IExchangeRateService exchangeRateService;

    @GetMapping
    public List<ExchangeRateResponseDto> getExchangeRates() {
        return exchangeRateService.getExchangeRates();
    }
}