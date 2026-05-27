package kopo.kstockcompass.service.impl;

import kopo.kstockcompass.dto.SimulatorDTO;
import kopo.kstockcompass.repository.SimulatorRepository;
import kopo.kstockcompass.repository.entity.SimulatorEntity;
import kopo.kstockcompass.service.ISimulatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulatorService implements ISimulatorService {

    private final SimulatorRepository simulatorRepository;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public List<SimulatorDTO> getSimulator(String userEmail) {
        return simulatorRepository.findByUserEmail(userEmail)
                .stream()
                .map(s -> {
                    BigDecimal avg = s.getAvgPrice();
                    BigDecimal target = s.getTargetPrice();
                    BigDecimal qty = BigDecimal.valueOf(s.getQuantity());

                    BigDecimal expectedRevenue = target.multiply(qty);
                    BigDecimal investAmt = avg.multiply(qty);
                    BigDecimal expectedProfit = expectedRevenue.subtract(investAmt);
                    BigDecimal expectedProfitRate = investAmt.compareTo(BigDecimal.ZERO) > 0
                            ? expectedProfit.divide(investAmt, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;

                    return new SimulatorDTO(
                            s.getSimId(),
                            s.getUserEmail(),
                            s.getStockCd(),
                            s.getStockNm(),
                            avg,
                            s.getQuantity(),
                            target,
                            s.getRegDt().format(DATE_FMT),
                            expectedRevenue,
                            investAmt,
                            expectedProfit,
                            expectedProfitRate
                    );
                })
                .toList();
    }

    @Override
    @Transactional
    public void addSimulator(String userEmail, String stockCd, String stockNm,
                             Long avgPrice, Long quantity, Long targetPrice) {
        SimulatorEntity entity = SimulatorEntity.builder()
                .userEmail(userEmail)
                .stockCd(stockCd)
                .stockNm(stockNm)
                .avgPrice(BigDecimal.valueOf(avgPrice))
                .quantity(quantity)
                .targetPrice(BigDecimal.valueOf(targetPrice))
                .build();
        simulatorRepository.save(entity);
        log.info("시뮬레이터 추가: {} - {} (목표가: {})", userEmail, stockCd, targetPrice);
    }

    @Override
    @Transactional
    public void updateSimulator(Long simId, String userEmail,
                                Long avgPrice, Long quantity, Long targetPrice) {
        simulatorRepository.findById(simId).ifPresent(s -> {
            if (s.getUserEmail().equals(userEmail)) {
                s.update(BigDecimal.valueOf(avgPrice),
                        quantity,
                        BigDecimal.valueOf(targetPrice));
                simulatorRepository.save(s);
                log.info("시뮬레이터 수정: simId={}", simId);
            }
        });
    }

    @Override
    @Transactional
    public void deleteSimulator(Long simId, String userEmail) {
        simulatorRepository.deleteBySimIdAndUserEmail(simId, userEmail);
        log.info("시뮬레이터 삭제: simId={}", simId);
    }
}