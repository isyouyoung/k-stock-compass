package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.FinancialDTO;

public interface IFinancialService {

    // AI_ANLS 3번
    // 종목코드로 DART 재무정보 조회
    FinancialDTO getFinancialData(String stockCode);
}