package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.MarketIndexDTO;
import kopo.kstockcompass.dto.StockItemDTO;
import kopo.kstockcompass.dto.StockSearchDTO;

import java.util.List;

public interface IStockService {

    StockItemDTO getStockPrice(String stockCode, String baseDate);
    // 여기서 getStockPirce에 커서놓고 컨트롤+알트+b 구현체로 이동

    void initStocks();

    void saveAllStocks();

    List<StockSearchDTO> searchStocks(String query, String type);
    // searchStock에서 컨트롤 알트 b 하면 실제 구현체로 이동됨

    MarketIndexDTO getMarketIndex(String idxNm, String baseDate);
    // getMarketIndex 컨트롤 알트 b 하면 실제 구현체로 이동됨

}