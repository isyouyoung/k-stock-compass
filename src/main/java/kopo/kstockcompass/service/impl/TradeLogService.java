package kopo.kstockcompass.service.impl;

import kopo.kstockcompass.dto.InvestmentGoalRequestDto;
import kopo.kstockcompass.dto.InvestmentGoalResponseDto;
import kopo.kstockcompass.dto.RequiredRateResponseDto;
import kopo.kstockcompass.dto.ScenarioRequestDto;
import kopo.kstockcompass.dto.TradeLogRequestDto;
import kopo.kstockcompass.dto.TradeLogResponseDto;
import kopo.kstockcompass.repository.TradeLogRepository;
import kopo.kstockcompass.repository.UserInvestmentGoalRepository;
import kopo.kstockcompass.repository.entity.TradeLogEntity;
import kopo.kstockcompass.repository.entity.UserInfoEntity;
import kopo.kstockcompass.repository.entity.UserInvestmentGoalEntity;
import kopo.kstockcompass.service.ITradeLogService;
import kopo.kstockcompass.util.EncryptUtil; // 💡 암호화 유틸 임포트 추가
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeLogService implements ITradeLogService {

    private final TradeLogRepository tradeLogRepository;
    private final UserInvestmentGoalRepository goalRepository;
    private final KisStockService kisStockService; // 현재가 조회용
    private final kopo.kstockcompass.repository.UserInfoRepository userInfoRepository;

    // 💡 핵심 해결책: 평문 이메일을 DB와 동일한 암호문으로 변환해주는 헬퍼 메서드
    private String getEncEmail(String plainEmail) {
        try {
            return EncryptUtil.encAES128CBC(plainEmail);
        } catch (Exception e) {
            log.error("이메일 암호화 처리 중 에러 발생", e);
            throw new RuntimeException("사용자 정보 처리 중 오류가 발생했습니다.");
        }
    }

    @Override
    @Transactional
    public TradeLogResponseDto registerTradeLog(String userEmail, TradeLogRequestDto dto) {
        String encEmail = getEncEmail(userEmail); // 💡 암호화

        UserInfoEntity user = userInfoRepository.findById(encEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        TradeLogEntity entity = TradeLogEntity.builder()
                .user(user)
                .stockCode(dto.stockCode())
                .stockName(dto.stockName())
                .buyPrice(dto.buyPrice())
                .quantity(dto.quantity())
                .build();

        TradeLogEntity saved = tradeLogRepository.save(entity);
        return TradeLogResponseDto.from(saved);
    }

    @Override
    public List<TradeLogResponseDto> getTradeLogList(String userEmail) {
        String encEmail = getEncEmail(userEmail); // 💡 암호화

        return tradeLogRepository.findByUser_UserEmailOrderByCreatedAtDesc(encEmail)
                .stream()
                .map(TradeLogResponseDto::from)
                .toList();
    }

    @Override
    @Transactional
    public void deleteTradeLog(String userEmail, Long tradeLogId) {
        String encEmail = getEncEmail(userEmail); // 💡 암호화

        TradeLogEntity entity = tradeLogRepository.findByIdAndUser_UserEmail(tradeLogId, encEmail)
                .orElseThrow(() -> new RuntimeException("본인의 매매일지만 삭제할 수 있습니다."));
        tradeLogRepository.delete(entity);
    }

    @Override
    @Transactional
    public InvestmentGoalResponseDto saveOrUpdateGoal(String userEmail, InvestmentGoalRequestDto dto) {
        String encEmail = getEncEmail(userEmail); // 💡 암호화

        UserInvestmentGoalEntity entity = goalRepository.findByUser_UserEmail(encEmail)
                .map(existing -> {
                    existing.updateTargetRate(dto.targetProfitRate());
                    return existing;
                })
                .orElseGet(() -> {
                    UserInfoEntity user = userInfoRepository.findById(encEmail)
                            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

                    UserInvestmentGoalEntity newEntity = UserInvestmentGoalEntity.builder()
                            .user(user)
                            .targetProfitRate(dto.targetProfitRate())
                            .build();
                    return goalRepository.save(newEntity);
                });

        return InvestmentGoalResponseDto.from(entity);
    }

    @Override
    public InvestmentGoalResponseDto getGoal(String userEmail) {
        String encEmail = getEncEmail(userEmail); // 💡 암호화

        return goalRepository.findByUser_UserEmail(encEmail)
                .map(InvestmentGoalResponseDto::from)
                .orElseGet(() -> InvestmentGoalResponseDto.builder()
                        .targetProfitRate(BigDecimal.ZERO)
                        .build());
    }

    @Override
    public RequiredRateResponseDto calculateRequiredRate(String userEmail) {
        String encEmail = getEncEmail(userEmail); // 💡 암호화

        BigDecimal targetRate = goalRepository.findByUser_UserEmail(encEmail)
                .map(UserInvestmentGoalEntity::getTargetProfitRate)
                .orElseThrow(() -> new RuntimeException("설정된 목표 수익률이 없습니다."));

        List<TradeLogEntity> logs = tradeLogRepository.findByUser_UserEmailOrderByCreatedAtDesc(encEmail);
        if (logs.isEmpty()) {
            throw new RuntimeException("등록된 매매일지가 없습니다.");
        }

        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        BigDecimal totalTargetValue = BigDecimal.ZERO;
        List<RequiredRateResponseDto.StockRequiredRate> stockRates = new ArrayList<>();

        for (TradeLogEntity logEntity : logs) {
            long currentPriceLong = kisStockService.getCurrentPrice(logEntity.getStockCode());
            BigDecimal currentPrice = BigDecimal.valueOf(currentPriceLong);
            BigDecimal currentValue = currentPrice.multiply(BigDecimal.valueOf(logEntity.getQuantity()));

            // 이 종목의 목표금액 = 매수원금 × (1 + 목표수익률/100)
            BigDecimal buyTotal = logEntity.getBuyPrice().multiply(BigDecimal.valueOf(logEntity.getQuantity()));
            BigDecimal targetValue = buyTotal.multiply(
                    BigDecimal.ONE.add(targetRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))
            );

            // 개별 종목 필요 상승률 = (목표금액 - 현재평가금액) / 현재평가금액 × 100
            BigDecimal stockRequiredRate = targetValue.subtract(currentValue)
                    .divide(currentValue, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            stockRates.add(RequiredRateResponseDto.StockRequiredRate.builder()
                    .stockCode(logEntity.getStockCode())
                    .stockName(logEntity.getStockName())
                    .currentPrice(currentPrice)
                    .currentValue(currentValue)
                    .requiredRate(stockRequiredRate.setScale(2, RoundingMode.HALF_UP))
                    .build());

            totalCurrentValue = totalCurrentValue.add(currentValue);
            totalTargetValue = totalTargetValue.add(targetValue);
        }

        BigDecimal overallRequiredRate = totalTargetValue.subtract(totalCurrentValue)
                .divide(totalCurrentValue, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return RequiredRateResponseDto.builder()
                .targetProfitRate(targetRate)
                .totalCurrentValue(totalCurrentValue)
                .totalTargetValue(totalTargetValue)
                .overallRequiredRate(overallRequiredRate.setScale(2, RoundingMode.HALF_UP))
                .stockRates(stockRates)
                .build();
    }

    @Override
    public RequiredRateResponseDto calculateScenarioRequiredRate(String userEmail, ScenarioRequestDto dto) {
        String encEmail = getEncEmail(userEmail); // 💡 암호화

        BigDecimal targetRate = goalRepository.findByUser_UserEmail(encEmail)
                .map(UserInvestmentGoalEntity::getTargetProfitRate)
                .orElseThrow(() -> new RuntimeException("설정된 목표 수익률이 없습니다."));

        List<TradeLogEntity> logs = tradeLogRepository.findByUser_UserEmailOrderByCreatedAtDesc(encEmail);
        if (logs.isEmpty()) {
            throw new RuntimeException("등록된 매매일지가 없습니다.");
        }

        Map<String, BigDecimal> fixedRates = dto.fixedRates();

        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        BigDecimal totalTargetValue = BigDecimal.ZERO;

        // 고정된(시나리오 입력된) 종목들의 목표 달성분
        BigDecimal fixedContribution = BigDecimal.ZERO;
        // 고정 안 된(나머지) 종목들의 현재평가금액 합
        BigDecimal remainingCurrentValue = BigDecimal.ZERO;

        List<RequiredRateResponseDto.StockRequiredRate> stockRates = new ArrayList<>();

        // 1단계: 전체 목표금액과 각 종목 현재가 계산
        for (TradeLogEntity logEntity : logs) {
            long currentPriceLong = kisStockService.getCurrentPrice(logEntity.getStockCode());
            BigDecimal currentPrice = BigDecimal.valueOf(currentPriceLong);
            BigDecimal currentValue = currentPrice.multiply(BigDecimal.valueOf(logEntity.getQuantity()));

            BigDecimal buyTotal = logEntity.getBuyPrice().multiply(BigDecimal.valueOf(logEntity.getQuantity()));
            BigDecimal targetValue = buyTotal.multiply(
                    BigDecimal.ONE.add(targetRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))
            );

            totalCurrentValue = totalCurrentValue.add(currentValue);
            totalTargetValue = totalTargetValue.add(targetValue);

            BigDecimal appliedRate = fixedRates.get(logEntity.getStockCode());
            if (appliedRate != null) {
                // 고정 상승률 적용된 종목 → 그 종목의 목표 달성 금액 계산
                BigDecimal fixedFutureValue = currentValue.multiply(
                        BigDecimal.ONE.add(appliedRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))
                );
                fixedContribution = fixedContribution.add(fixedFutureValue);
            } else {
                // 나머지 종목 → 필요 상승률을 나중에 역산
                remainingCurrentValue = remainingCurrentValue.add(currentValue);
            }

            stockRates.add(RequiredRateResponseDto.StockRequiredRate.builder()
                    .stockCode(logEntity.getStockCode())
                    .stockName(logEntity.getStockName())
                    .currentPrice(currentPrice)
                    .currentValue(currentValue)
                    .requiredRate(appliedRate) // 고정 종목은 여기서 채워짐, 나머지는 null
                    .build());
        }

        // 2단계: 나머지 종목들의 균등 필요 상승률 역산
        // (전체 목표금액 - 고정 종목 달성금액) / 나머지 종목 현재평가금액 합 × 100 - 100
        BigDecimal finalRemainingRate;
        if (remainingCurrentValue.compareTo(BigDecimal.ZERO) == 0) {
            finalRemainingRate = BigDecimal.ZERO; // 전 종목이 고정된 경우
        } else {
            finalRemainingRate = totalTargetValue.subtract(fixedContribution)
                    .divide(remainingCurrentValue, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .subtract(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // 나머지 종목들에 계산된 필요 상승률 채워넣기
        List<RequiredRateResponseDto.StockRequiredRate> finalStockRates = new ArrayList<>();
        for (RequiredRateResponseDto.StockRequiredRate rate : stockRates) {
            if (rate.requiredRate() == null) {
                finalStockRates.add(RequiredRateResponseDto.StockRequiredRate.builder()
                        .stockCode(rate.stockCode())
                        .stockName(rate.stockName())
                        .currentPrice(rate.currentPrice())
                        .currentValue(rate.currentValue())
                        .requiredRate(finalRemainingRate)
                        .build());
            } else {
                finalStockRates.add(rate);
            }
        }

        BigDecimal overallRequiredRate = totalTargetValue.subtract(totalCurrentValue)
                .divide(totalCurrentValue, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return RequiredRateResponseDto.builder()
                .targetProfitRate(targetRate)
                .totalCurrentValue(totalCurrentValue)
                .totalTargetValue(totalTargetValue)
                .overallRequiredRate(overallRequiredRate.setScale(2, RoundingMode.HALF_UP))
                .stockRates(finalStockRates)
                .build();
    }
}