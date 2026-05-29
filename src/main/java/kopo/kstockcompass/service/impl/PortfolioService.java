package kopo.kstockcompass.service.impl;

import kopo.kstockcompass.dto.AccountDTO;
import kopo.kstockcompass.dto.PortfolioDTO;
import kopo.kstockcompass.repository.AccountRepository;
import kopo.kstockcompass.repository.PortfolioRepository;
import kopo.kstockcompass.repository.StockRepository;
import kopo.kstockcompass.repository.entity.AccountEntity;
import kopo.kstockcompass.repository.entity.PortfolioEntity;
import kopo.kstockcompass.service.IPortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * [포트폴리오 서비스]
 * 역할:
 * - 사용자의 보유 주식 포트폴리오 관리
 * - 실시간 현재가 기반 평가금액/수익률 계산
 * - 계좌(현금/대출) 정보 관리
 *
 * 특징:
 * - KIS API를 통해 실시간 가격 조회
 * - BigDecimal 기반 정밀한 수익 계산
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService implements IPortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final AccountRepository accountRepository;
    private final StockRepository stockRepository;
    private final KisStockService kisStockService;

    // 화면 표시용 날짜 포맷 (UI 정렬/가독성 목적)
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * [포트폴리오 조회]
     * 역할:
     * - 사용자의 보유 종목 목록 조회
     * - 각 종목별 현재가(KIS API) 조회
     * - 평가금액 / 손익 / 수익률 계산
     *
     * 흐름:
     * DB 조회 → 종목명 매핑 → 실시간 현재가 조회 → 계산 → DTO 반환
     */
    @Override
    public List<PortfolioDTO> getPortfolio(String userEmail) {
        return portfolioRepository.findByUserEmail(userEmail)
                .stream()
                .map(p -> {

                    // 종목 코드 → 종목명 변환 (없으면 코드 그대로 사용)
                    String stockNm = stockRepository.findById(p.getStockCd())
                            .map(s -> s.getStockNm())
                            .orElse(p.getStockCd());

                    // KIS API로 실시간 현재가 조회
                    long currentPrice = 0;
                    try {
                        currentPrice = kisStockService.getCurrentPrice(p.getStockCd());
                    } catch (Exception e) {
                        log.warn("현재가 조회 실패: {}", p.getStockCd());
                    }

                    // BigDecimal 기반 정밀 계산 (금융 데이터 오차 방지)
                    BigDecimal current = BigDecimal.valueOf(currentPrice);
                    BigDecimal qty = BigDecimal.valueOf(p.getQuantity());

                    // 평가 금액 = 현재가 * 수량
                    BigDecimal evalAmt = current.multiply(qty);

                    // 매수 금액 = 평균단가 * 수량
                    BigDecimal investAmt = p.getAvgPrice().multiply(qty);

                    // 손익 = 평가금액 - 매수금액
                    BigDecimal profitAmt = evalAmt.subtract(investAmt);

                    // 수익률 = (손익 / 매수금액) * 100
                    BigDecimal profitRate = investAmt.compareTo(BigDecimal.ZERO) > 0
                            ? profitAmt.divide(investAmt, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;

                    return new PortfolioDTO(
                            p.getPortId(),
                            p.getUserEmail(),
                            p.getStockCd(),
                            stockNm,
                            p.getAvgPrice(),
                            p.getQuantity(),
                            p.getRegDt().format(DATE_FMT),
                            currentPrice,
                            evalAmt,
                            profitAmt,
                            profitRate
                    );
                })
                .toList();
    }

    /**
     * [포트폴리오 추가]
     * 역할:
     * - 사용자가 보유 종목을 신규 등록
     */
    @Override
    @Transactional
    public void addPortfolio(String userEmail, String stockCd, Long avgPrice, Long quantity) {
        PortfolioEntity entity = PortfolioEntity.builder()
                .userEmail(userEmail)
                .stockCd(stockCd)
                .avgPrice(BigDecimal.valueOf(avgPrice))
                .quantity(quantity)
                .build();

        portfolioRepository.save(entity);

        log.info("포트폴리오 추가: {} - {} ({}주 @ {}원)", userEmail, stockCd, quantity, avgPrice);
    }

    /**
     * [포트폴리오 삭제]
     * 역할:
     * - 특정 포트폴리오 항목 삭제 (본인 데이터만)
     */
    @Override
    @Transactional
    public void deletePortfolio(Long portId, String userEmail) {
        portfolioRepository.deleteByPortIdAndUserEmail(portId, userEmail);
        log.info("포트폴리오 삭제: portId={}", portId);
    }

    /**
     * [계좌 조회]
     * 역할:
     * - 사용자 현금 / 대출 정보 조회
     */
    @Override
    public AccountDTO getAccount(String userEmail) {
        return accountRepository.findById(userEmail)
                .map(a -> new AccountDTO(
                        a.getUserEmail(),
                        a.getCash(),
                        a.getLoan()
                ))
                .orElse(new AccountDTO(
                        userEmail,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ));
    }

    /**
     * [계좌 업데이트]
     * 역할:
     * - 예수금 / 대출 금액 수정
     *
     * 특징:
     * - 기존 계좌 없으면 자동 생성
     * - updateBalance()로 엔티티 내부 값 변경
     */
    @Override
    @Transactional
    public void updateAccount(String userEmail, Long cash, Long loan) {

        AccountEntity entity = accountRepository.findById(userEmail)
                .orElse(AccountEntity.builder()
                        .userEmail(userEmail)
                        .cash(BigDecimal.ZERO)
                        .loan(BigDecimal.ZERO)
                        .build());

        entity.updateBalance(
                BigDecimal.valueOf(cash),
                BigDecimal.valueOf(loan)
        );

        accountRepository.save(entity);

        log.info("계좌 수정: {} - 예수금: {}, 대출: {}", userEmail, cash, loan);
    }
}