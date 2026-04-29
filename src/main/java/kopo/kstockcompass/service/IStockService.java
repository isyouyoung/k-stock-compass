package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.MarketIndexDTO;
import kopo.kstockcompass.dto.StockItemDTO;
import kopo.kstockcompass.dto.StockSearchDTO;

import java.util.List;

public interface IStockService {

    StockItemDTO getStockPrice(String stockCode, String baseDate);

    void initStocks();

    void saveAllStocks();

    List<StockSearchDTO> searchStocks(String keyword);

    MarketIndexDTO getMarketIndex(String idxNm, String baseDate);
}