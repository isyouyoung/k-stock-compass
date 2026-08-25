package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.InvestmentGoalRequestDto;
import kopo.kstockcompass.dto.InvestmentGoalResponseDto;
import kopo.kstockcompass.dto.RequiredRateResponseDto;
import kopo.kstockcompass.dto.ScenarioRequestDto;
import kopo.kstockcompass.dto.TradeLogRequestDto;
import kopo.kstockcompass.dto.TradeLogResponseDto;

import java.util.List;

public interface ITradeLogService {

    // 매매일지 등록
    TradeLogResponseDto registerTradeLog(String userEmail, TradeLogRequestDto dto);

    // 매매일지 전체 조회
    List<TradeLogResponseDto> getTradeLogList(String userEmail);

    // 매매일지 삭제 (본인 확인 포함)
    void deleteTradeLog(String userEmail, Long tradeLogId);

    // 목표 수익률 저장/수정 (Upsert)
    InvestmentGoalResponseDto saveOrUpdateGoal(String userEmail, InvestmentGoalRequestDto dto);

    // 목표 수익률 조회
    InvestmentGoalResponseDto getGoal(String userEmail);

    // 전체 계좌 균등 필요 상승률 계산
    RequiredRateResponseDto calculateRequiredRate(String userEmail);

    // 시나리오 기반 필요 상승률 계산 (일부 종목 고정)
    RequiredRateResponseDto calculateScenarioRequiredRate(String userEmail, ScenarioRequestDto dto);
}