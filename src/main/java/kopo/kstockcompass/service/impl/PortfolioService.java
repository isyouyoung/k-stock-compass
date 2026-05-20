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

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService implements IPortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final AccountRepository accountRepository;
    private final StockRepository stockRepository;
    private final KisStockService kisStockService;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public List<PortfolioDTO> getPortfolio(String userEmail) {
        return portfolioRepository.findByUserEmail(userEmail)
                .stream()
                .map(p -> {
                    String stockNm = stockRepository.findById(p.getStockCd())
                            .map(s -> s.getStockNm())
                            .orElse(p.getStockCd());

                    // KIS API로 현재가 조회
                    long currentPrice = 0;
                    try {
                        currentPrice = kisStockService.getCurrentPrice(p.getStockCd());
                    } catch (Exception e) {
                        log.warn("현재가 조회 실패: {}", p.getStockCd());
                    }

                    BigDecimal current = BigDecimal.valueOf(currentPrice);
                    BigDecimal qty = BigDecimal.valueOf(p.getQuantity());
                    BigDecimal evalAmt = current.multiply(qty);
                    BigDecimal investAmt = p.getAvgPrice().multiply(qty);
                    BigDecimal profitAmt = evalAmt.subtract(investAmt);
                    BigDecimal profitRate = investAmt.compareTo(BigDecimal.ZERO) > 0
                            ? profitAmt.divide(investAmt, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;

                    return PortfolioDTO.builder()
                            .portId(p.getPortId())
                            .userEmail(p.getUserEmail())
                            .stockCd(p.getStockCd())
                            .stockNm(stockNm)
                            .avgPrice(p.getAvgPrice())
                            .quantity(p.getQuantity())
                            .regDt(p.getRegDt().format(DATE_FMT))
                            .currentPrice(currentPrice)
                            .evalAmt(evalAmt)
                            .profitAmt(profitAmt)
                            .profitRate(profitRate)
                            .build();
                })
                .toList();
    }

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

    @Override
    @Transactional
    public void deletePortfolio(Long portId, String userEmail) {
        portfolioRepository.deleteByPortIdAndUserEmail(portId, userEmail);
        log.info("포트폴리오 삭제: portId={}", portId);
    }

    @Override
    public AccountDTO getAccount(String userEmail) {
        return accountRepository.findById(userEmail)
                .map(a -> AccountDTO.builder()
                        .userEmail(a.getUserEmail())
                        .cash(a.getCash())
                        .loan(a.getLoan())
                        .build())
                .orElse(AccountDTO.builder()
                        .userEmail(userEmail)
                        .cash(BigDecimal.ZERO)
                        .loan(BigDecimal.ZERO)
                        .build());
    }

    @Override
    @Transactional
    public void updateAccount(String userEmail, Long cash, Long loan) {
        AccountEntity entity = accountRepository.findById(userEmail)
                .orElse(AccountEntity.builder()
                        .userEmail(userEmail)
                        .cash(BigDecimal.ZERO)
                        .loan(BigDecimal.ZERO)
                        .build());
        entity.updateBalance(BigDecimal.valueOf(cash), BigDecimal.valueOf(loan));
        accountRepository.save(entity);
        log.info("계좌 수정: {} - 예수금: {}, 대출: {}", userEmail, cash, loan);
    }
}