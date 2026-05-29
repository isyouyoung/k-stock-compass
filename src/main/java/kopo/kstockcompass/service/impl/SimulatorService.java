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

    // 화면 출력용 날짜 포맷 (시뮬레이션 생성 시점 표시)
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * [시뮬레이터 조회]
     * 역할:
     * - 사용자가 설정한 목표가 기반 투자 시뮬레이션 결과 조회
     *
     * 핵심:
     * - 현재가가 아니라 "목표가 기준 예상 수익" 계산
     * - 투자금 대비 수익률을 미리 시뮬레이션
     */
    @Override
    public List<SimulatorDTO> getSimulator(String userEmail) {
        return simulatorRepository.findByUserEmail(userEmail)
                .stream()
                .map(s -> {

                    // 평균 매수가
                    BigDecimal avg = s.getAvgPrice();

                    // 목표가 (사용자가 설정한 미래 기대 가격)
                    BigDecimal target = s.getTargetPrice();

                    // 수량
                    BigDecimal qty = BigDecimal.valueOf(s.getQuantity());

                    // 목표가 기준 예상 매도 금액
                    BigDecimal expectedRevenue = target.multiply(qty);

                    // 실제 투자 금액 (매수 기준)
                    BigDecimal investAmt = avg.multiply(qty);

                    // 예상 손익 = 목표 매도금액 - 투자금
                    BigDecimal expectedProfit = expectedRevenue.subtract(investAmt);

                    // 예상 수익률 = (손익 / 투자금) * 100
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

    /**
     * [시뮬레이터 등록]
     * 역할:
     * - 사용자가 특정 종목의 목표가를 설정하여 투자 시뮬레이션 생성
     */
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

    /**
     * [시뮬레이터 수정]
     * 역할:
     * - 기존 시뮬레이션 조건 (평균가 / 수량 / 목표가) 수정
     *
     * 보안:
     * - 본인 데이터인지 확인 후 업데이트 수행
     */
    @Override
    @Transactional
    public void updateSimulator(Long simId, String userEmail,
                                Long avgPrice, Long quantity, Long targetPrice) {

        simulatorRepository.findById(simId).ifPresent(s -> {
            if (s.getUserEmail().equals(userEmail)) {

                // 엔티티 내부 update 메서드를 통한 값 변경
                s.update(
                        BigDecimal.valueOf(avgPrice),
                        quantity,
                        BigDecimal.valueOf(targetPrice)
                );

                simulatorRepository.save(s);

                log.info("시뮬레이터 수정: simId={}", simId);
            }
        });
    }

    /**
     * [시뮬레이터 삭제]
     * 역할:
     * - 사용자의 시뮬레이션 데이터 삭제
     *
     * 보안:
     * - userEmail 조건 포함 (타인 삭제 방지)
     */
    @Override
    @Transactional
    public void deleteSimulator(Long simId, String userEmail) {
        simulatorRepository.deleteBySimIdAndUserEmail(simId, userEmail);
        log.info("시뮬레이터 삭제: simId={}", simId);
    }
}