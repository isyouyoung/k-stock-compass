package kopo.kstockcompass.controller;

import kopo.kstockcompass.dto.InvestmentGoalRequestDto;
import kopo.kstockcompass.dto.InvestmentGoalResponseDto;
import kopo.kstockcompass.dto.RequiredRateResponseDto;
import kopo.kstockcompass.dto.ScenarioRequestDto;
import kopo.kstockcompass.dto.TradeLogRequestDto;
import kopo.kstockcompass.dto.TradeLogResponseDto;
import kopo.kstockcompass.service.ITradeLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/trade-log")
@RequiredArgsConstructor
public class TradeLogController {

    private final ITradeLogService tradeLogService;

    // 매매일지 등록
    @PostMapping
    public TradeLogResponseDto registerTradeLog(
            @AuthenticationPrincipal String userEmail,
            @RequestBody TradeLogRequestDto dto) {
        return tradeLogService.registerTradeLog(userEmail, dto);
    }

    // 매매일지 전체 조회
    @GetMapping
    public List<TradeLogResponseDto> getTradeLogList(
            @AuthenticationPrincipal String userEmail) {
        return tradeLogService.getTradeLogList(userEmail);
    }

    // 매매일지 삭제
    @DeleteMapping("/{tradeLogId}")
    public void deleteTradeLog(
            @AuthenticationPrincipal String userEmail,
            @PathVariable Long tradeLogId) {
        tradeLogService.deleteTradeLog(userEmail, tradeLogId);
    }

    // 목표 수익률 저장/수정
    @PostMapping("/goal")
    public InvestmentGoalResponseDto saveOrUpdateGoal(
            @AuthenticationPrincipal String userEmail,
            @RequestBody InvestmentGoalRequestDto dto) {
        return tradeLogService.saveOrUpdateGoal(userEmail, dto);
    }

    // 목표 수익률 조회
    @GetMapping("/goal")
    public InvestmentGoalResponseDto getGoal(
            @AuthenticationPrincipal String userEmail) {
        return tradeLogService.getGoal(userEmail);
    }

    // 전체 계좌 균등 필요 상승률 계산
    @GetMapping("/required-rate")
    public RequiredRateResponseDto getRequiredRate(
            @AuthenticationPrincipal String userEmail) {
        return tradeLogService.calculateRequiredRate(userEmail);
    }

    // 시나리오 기반 필요 상승률 계산 (일부 종목 고정)
    @PostMapping("/required-rate/scenario")
    public RequiredRateResponseDto getScenarioRequiredRate(
            @AuthenticationPrincipal String userEmail,
            @RequestBody ScenarioRequestDto dto) {
        return tradeLogService.calculateScenarioRequiredRate(userEmail, dto);
    }
}