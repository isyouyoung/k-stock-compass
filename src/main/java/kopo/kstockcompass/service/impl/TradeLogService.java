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
import kopo.kstockcompass.util.EncryptUtil;
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
    private final KisStockService kisStockService;
    private final kopo.kstockcompass.repository.UserInfoRepository userInfoRepository;

    // 평문 이메일을 DB와 동일한 암호문으로 변환
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
    public TradeLogResponseDto registerTradeLog(
            String userEmail,
            TradeLogRequestDto dto
    ) {
        String encEmail = getEncEmail(userEmail);

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
        String encEmail = getEncEmail(userEmail);

        return tradeLogRepository
                .findByUser_UserEmailOrderByCreatedAtDesc(encEmail)
                .stream()
                .map(TradeLogResponseDto::from)
                .toList();
    }

    @Override
    @Transactional
    public void deleteTradeLog(String userEmail, Long tradeLogId) {
        String encEmail = getEncEmail(userEmail);

        TradeLogEntity entity =
                tradeLogRepository.findByIdAndUser_UserEmail(
                                tradeLogId,
                                encEmail
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "본인의 매매일지만 삭제할 수 있습니다."
                                )
                        );

        tradeLogRepository.delete(entity);
    }

    @Override
    @Transactional
    public InvestmentGoalResponseDto saveOrUpdateGoal(
            String userEmail,
            InvestmentGoalRequestDto dto
    ) {
        String encEmail = getEncEmail(userEmail);

        UserInvestmentGoalEntity entity =
                goalRepository.findByUser_UserEmail(encEmail)
                        .map(existing -> {
                            existing.updateTargetRate(
                                    dto.targetProfitRate()
                            );
                            return existing;
                        })
                        .orElseGet(() -> {
                            UserInfoEntity user =
                                    userInfoRepository.findById(encEmail)
                                            .orElseThrow(() ->
                                                    new RuntimeException(
                                                            "사용자를 찾을 수 없습니다."
                                                    )
                                            );

                            UserInvestmentGoalEntity newEntity =
                                    UserInvestmentGoalEntity.builder()
                                            .user(user)
                                            .targetProfitRate(
                                                    dto.targetProfitRate()
                                            )
                                            .build();

                            return goalRepository.save(newEntity);
                        });

        return InvestmentGoalResponseDto.from(entity);
    }

    @Override
    public InvestmentGoalResponseDto getGoal(String userEmail) {
        String encEmail = getEncEmail(userEmail);

        return goalRepository.findByUser_UserEmail(encEmail)
                .map(InvestmentGoalResponseDto::from)
                .orElseGet(() ->
                        InvestmentGoalResponseDto.builder()
                                .targetProfitRate(BigDecimal.ZERO)
                                .updatedAt("")
                                .build()
                );
    }

    /**
     * 전체 목표 수익률을 달성하기 위해 필요한 수익금을 계산하고,
     * 각 종목이 그 수익금을 혼자 만들어낸다고 가정했을 때
     * 필요한 상승률을 계산한다.
     */
    @Override
    public RequiredRateResponseDto calculateRequiredRate(String userEmail) {

        String encEmail = getEncEmail(userEmail);

        BigDecimal targetRate =
                goalRepository.findByUser_UserEmail(encEmail)
                        .map(UserInvestmentGoalEntity::getTargetProfitRate)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "설정된 목표 수익률이 없습니다."
                                )
                        );

        List<TradeLogEntity> logs =
                tradeLogRepository
                        .findByUser_UserEmailOrderByCreatedAtDesc(encEmail);

        if (logs.isEmpty()) {
            throw new RuntimeException(
                    "등록된 매매일지가 없습니다."
            );
        }

        // 전체 매수원금
        BigDecimal totalBuyAmount = BigDecimal.ZERO;

        // 전체 현재 평가금액
        BigDecimal totalCurrentValue = BigDecimal.ZERO;

        List<RequiredRateResponseDto.StockRequiredRate> stockRates =
                new ArrayList<>();

        // --------------------------------------------------
        // 1. 각 종목의 현재 평가금액과 전체 매수원금 계산
        // --------------------------------------------------
        for (TradeLogEntity logEntity : logs) {

            long currentPriceLong =
                    kisStockService.getCurrentPrice(
                            logEntity.getStockCode()
                    );

            BigDecimal currentPrice =
                    BigDecimal.valueOf(currentPriceLong);

            BigDecimal currentValue =
                    currentPrice.multiply(
                            BigDecimal.valueOf(
                                    logEntity.getQuantity()
                            )
                    );

            BigDecimal buyTotal =
                    logEntity.getBuyPrice().multiply(
                            BigDecimal.valueOf(
                                    logEntity.getQuantity()
                            )
                    );

            totalBuyAmount =
                    totalBuyAmount.add(buyTotal);

            totalCurrentValue =
                    totalCurrentValue.add(currentValue);

            stockRates.add(
                    RequiredRateResponseDto.StockRequiredRate.builder()
                            .stockCode(logEntity.getStockCode())
                            .stockName(logEntity.getStockName())
                            .currentPrice(currentPrice)
                            .currentValue(currentValue)
                            .build()
            );
        }

        // --------------------------------------------------
        // 2. 전체 목표 평가금액
        //
        // 예:
        // 매수원금 1,110,000원
        // 목표수익률 10%
        // → 목표 평가금액 1,221,000원
        // --------------------------------------------------
        BigDecimal totalTargetValue =
                totalBuyAmount.multiply(
                        BigDecimal.ONE.add(
                                targetRate.divide(
                                        BigDecimal.valueOf(100),
                                        6,
                                        RoundingMode.HALF_UP
                                )
                        )
                );

        // --------------------------------------------------
        // 3. 현재부터 추가로 필요한 전체 수익금
        // --------------------------------------------------
        BigDecimal totalRequiredProfit =
                totalTargetValue.subtract(totalCurrentValue);

        // --------------------------------------------------
        // 4. 각 종목이 전체 필요한 수익금을
        //    혼자 만들어낸다고 가정
        // --------------------------------------------------
        List<RequiredRateResponseDto.StockRequiredRate>
                finalStockRates = new ArrayList<>();

        for (RequiredRateResponseDto.StockRequiredRate stock
                : stockRates) {

            BigDecimal currentValue =
                    stock.currentValue();

            BigDecimal requiredRate;

            if (currentValue.compareTo(BigDecimal.ZERO) == 0) {

                requiredRate = BigDecimal.ZERO;

            } else {

                requiredRate =
                        totalRequiredProfit
                                .divide(
                                        currentValue,
                                        6,
                                        RoundingMode.HALF_UP
                                )
                                .multiply(
                                        BigDecimal.valueOf(100)
                                );
            }

            finalStockRates.add(
                    RequiredRateResponseDto.StockRequiredRate.builder()
                            .stockCode(stock.stockCode())
                            .stockName(stock.stockName())
                            .currentPrice(stock.currentPrice())
                            .currentValue(stock.currentValue())
                            .requiredRate(
                                    requiredRate.setScale(
                                            2,
                                            RoundingMode.HALF_UP
                                    )
                            )
                            .build()
            );
        }

        return RequiredRateResponseDto.builder()
                .targetProfitRate(targetRate)
                .totalCurrentValue(totalCurrentValue)
                .totalTargetValue(totalTargetValue)
                .totalRequiredProfit(totalRequiredProfit)
                .stockRates(finalStockRates)
                .build();
    }

    /**
     * 시나리오별 필요 상승률 계산
     *
     * 기존 기능을 유지하면서 새로운 DTO 구조에 맞게 반환한다.
     */
    @Override
    public RequiredRateResponseDto calculateScenarioRequiredRate(
            String userEmail,
            ScenarioRequestDto dto
    ) {

        String encEmail = getEncEmail(userEmail);

        BigDecimal targetRate =
                goalRepository.findByUser_UserEmail(encEmail)
                        .map(UserInvestmentGoalEntity::getTargetProfitRate)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "설정된 목표 수익률이 없습니다."
                                )
                        );

        List<TradeLogEntity> logs =
                tradeLogRepository
                        .findByUser_UserEmailOrderByCreatedAtDesc(
                                encEmail
                        );

        if (logs.isEmpty()) {
            throw new RuntimeException(
                    "등록된 매매일지가 없습니다."
            );
        }

        Map<String, BigDecimal> fixedRates =
                dto.fixedRates();

        BigDecimal totalCurrentValue =
                BigDecimal.ZERO;

        BigDecimal totalTargetValue =
                BigDecimal.ZERO;

        BigDecimal fixedContribution =
                BigDecimal.ZERO;

        BigDecimal remainingCurrentValue =
                BigDecimal.ZERO;

        List<RequiredRateResponseDto.StockRequiredRate>
                stockRates = new ArrayList<>();

        // --------------------------------------------------
        // 1. 종목별 현재 평가금액 / 목표금액 계산
        // --------------------------------------------------
        for (TradeLogEntity logEntity : logs) {

            long currentPriceLong =
                    kisStockService.getCurrentPrice(
                            logEntity.getStockCode()
                    );

            BigDecimal currentPrice =
                    BigDecimal.valueOf(currentPriceLong);

            BigDecimal currentValue =
                    currentPrice.multiply(
                            BigDecimal.valueOf(
                                    logEntity.getQuantity()
                            )
                    );

            BigDecimal buyTotal =
                    logEntity.getBuyPrice().multiply(
                            BigDecimal.valueOf(
                                    logEntity.getQuantity()
                            )
                    );

            BigDecimal targetValue =
                    buyTotal.multiply(
                            BigDecimal.ONE.add(
                                    targetRate.divide(
                                            BigDecimal.valueOf(100),
                                            6,
                                            RoundingMode.HALF_UP
                                    )
                            )
                    );

            totalCurrentValue =
                    totalCurrentValue.add(currentValue);

            totalTargetValue =
                    totalTargetValue.add(targetValue);

            BigDecimal appliedRate =
                    fixedRates.get(
                            logEntity.getStockCode()
                    );

            if (appliedRate != null) {

                BigDecimal fixedFutureValue =
                        currentValue.multiply(
                                BigDecimal.ONE.add(
                                        appliedRate.divide(
                                                BigDecimal.valueOf(100),
                                                6,
                                                RoundingMode.HALF_UP
                                        )
                                )
                        );

                fixedContribution =
                        fixedContribution.add(
                                fixedFutureValue
                        );

            } else {

                remainingCurrentValue =
                        remainingCurrentValue.add(
                                currentValue
                        );
            }

            stockRates.add(
                    RequiredRateResponseDto.StockRequiredRate.builder()
                            .stockCode(logEntity.getStockCode())
                            .stockName(logEntity.getStockName())
                            .currentPrice(currentPrice)
                            .currentValue(currentValue)
                            .requiredRate(appliedRate)
                            .build()
            );
        }

        // --------------------------------------------------
        // 2. 고정되지 않은 종목들의 필요 상승률 계산
        // --------------------------------------------------
        BigDecimal finalRemainingRate;

        if (remainingCurrentValue.compareTo(
                BigDecimal.ZERO
        ) == 0) {

            finalRemainingRate =
                    BigDecimal.ZERO;

        } else {

            finalRemainingRate =
                    totalTargetValue
                            .subtract(fixedContribution)
                            .divide(
                                    remainingCurrentValue,
                                    6,
                                    RoundingMode.HALF_UP
                            )
                            .multiply(
                                    BigDecimal.valueOf(100)
                            )
                            .subtract(
                                    BigDecimal.valueOf(100)
                            )
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );
        }

        // --------------------------------------------------
        // 3. 나머지 종목에 계산된 상승률 적용
        // --------------------------------------------------
        List<RequiredRateResponseDto.StockRequiredRate>
                finalStockRates = new ArrayList<>();

        for (RequiredRateResponseDto.StockRequiredRate rate
                : stockRates) {

            if (rate.requiredRate() == null) {

                finalStockRates.add(
                        RequiredRateResponseDto.StockRequiredRate.builder()
                                .stockCode(rate.stockCode())
                                .stockName(rate.stockName())
                                .currentPrice(rate.currentPrice())
                                .currentValue(rate.currentValue())
                                .requiredRate(finalRemainingRate)
                                .build()
                );

            } else {

                finalStockRates.add(rate);
            }
        }

        // --------------------------------------------------
        // 4. 새로운 DTO 구조에 맞는 전체 필요 수익금
        // --------------------------------------------------
        BigDecimal totalRequiredProfit =
                totalTargetValue.subtract(
                        totalCurrentValue
                );

        return RequiredRateResponseDto.builder()
                .targetProfitRate(targetRate)
                .totalCurrentValue(totalCurrentValue)
                .totalTargetValue(totalTargetValue)
                .totalRequiredProfit(totalRequiredProfit)
                .stockRates(finalStockRates)
                .build();
    }
}