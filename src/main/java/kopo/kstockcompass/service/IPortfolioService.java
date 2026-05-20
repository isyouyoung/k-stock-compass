package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.AccountDTO;
import kopo.kstockcompass.dto.PortfolioDTO;

import java.util.List;

public interface IPortfolioService {

    // 보유종목 조회
    List<PortfolioDTO> getPortfolio(String userEmail);

    // 보유종목 추가
    void addPortfolio(String userEmail, String stockCd, Long avgPrice, Long quantity);

    // 보유종목 삭제
    void deletePortfolio(Long portId, String userEmail);

    // 예수금/대출금 조회
    AccountDTO getAccount(String userEmail);

    // 예수금/대출금 수정
    void updateAccount(String userEmail, Long cash, Long loan);
}