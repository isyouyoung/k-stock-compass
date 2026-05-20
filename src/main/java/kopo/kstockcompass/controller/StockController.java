package kopo.kstockcompass.controller;

import kopo.kstockcompass.dto.MarketIndexDTO;
import kopo.kstockcompass.dto.StockItemDTO;
import kopo.kstockcompass.dto.StockSearchDTO;
import kopo.kstockcompass.service.IStockService;
import kopo.kstockcompass.service.impl.KisStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import kopo.kstockcompass.repository.StockRepository;

import java.util.List;

// 이 컨트롤러는 주식 정보를 가져오고 검색하는 안내데스크
// 교수님께서 강조하신 대로 모든 반환 값은 엔티티가 아니라 'DTO'로 전송
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stock")
public class StockController {

    private final IStockService stockService;
    private final KisStockService kisStockService;
    private final StockRepository stockRepository;


    /**
     * [주식 시세 조회]
     * 종목코드랑 날짜를 받아서 그날의 가격 정보를 가져오는 곳
     * 여기서 반환하는 'StockItemDTO' 안에는 시가, 종가, 거래량 같은 정보가 들어있음
     * DB(엔티티) 구조를 그대로 보여주는 게 아니라 화면에 필요한 것만 골라 담은 DTO를 반환
     */
    @GetMapping("/price")
    public ResponseEntity<StockItemDTO> getStockPrice(
            @RequestParam String stockCode,
            @RequestParam String baseDate) {
        StockItemDTO result = stockService.getStockPrice(stockCode, baseDate);
        // GetStockPrice에 커서 놓고 컨트롤 + B로 I서비스 진입
        return ResponseEntity.ok(result);
    }

    /**
     * [종목 초기화 및 수집]
     * 이건 우리 DB를 최신 종목 리스트로 꽉 채움
     * 공공데이터 API에서 2,800개가 넘는 종목을 긁어오기 때문에 처음에 한 번만 실행
     * 이 안에서 돌아가는 로직은 RestTemplate 말고 WebClient를 쓰고 있음 <= 교수님 지시사항
     */
    @PostMapping("/init")
    public ResponseEntity<String> initStocks() {
        stockService.initStocks();
        return ResponseEntity.ok("종목 초기화 및 수집이 완료되었습니다.");
    }

    /**
     * [종목 검색]
     * 사용자가 검색창에 '삼성'이라고 치면 DB에서 종목들을 찾아주는 기능
     * 검색 결과도 StockSearchDTO 리스트로 보내줌으로써 DB 보안을 유지
     */
    @GetMapping("/search")
    public ResponseEntity<List<StockSearchDTO>> searchStocks(
            @RequestParam String query,
            @RequestParam String type) {
        return ResponseEntity.ok(stockService.searchStocks(query, type));
    }

    /**
     * [지수 정보 조회]
     * 코스피(KOSPI)나 코스닥(KOSDAQ) 같은 시장 지수 정보를 가져옴
     * 역시 MarketIndexDTO라는 전용 가방을 써서 데이터만 전달
     */
    @GetMapping("/index")
    public ResponseEntity<MarketIndexDTO> getMarketIndex(
            @RequestParam String idxNm,
            @RequestParam String baseDate) {
        return ResponseEntity.ok(stockService.getMarketIndex(idxNm, baseDate));
        // 서비스 호출!! 리턴쪽에 getMarketIndex 컨트롤 + B로 타볼것
    }

    @GetMapping("/kis-price")
    public ResponseEntity<Long> getKisPrice(@RequestParam String stockCode) {
        return ResponseEntity.ok(kisStockService.getCurrentPrice(stockCode));
    }

    @GetMapping("/detail")
    public ResponseEntity<StockItemDTO> getStockDetail(@RequestParam String stockCode) {
        StockItemDTO kisData = kisStockService.getStockDetail(stockCode);

        String stockNm = stockRepository.findById(stockCode)
                .map(s -> s.getStockNm())
                .orElse(stockCode);

        String mrktCtg = stockRepository.findById(stockCode)
                .map(s -> s.getMktType())
                .orElse("KOSPI");

        StockItemDTO result = StockItemDTO.builder()
                .srtnCd(stockCode)
                .itmsNm(stockNm)
                .clpr(kisData.getClpr())
                .vs(kisData.getVs())
                .fltRt(kisData.getFltRt())
                .mrktCtg(mrktCtg)
                .oprc(kisData.getOprc())
                .hgpr(kisData.getHgpr())
                .lwpr(kisData.getLwpr())
                .acmlVol(kisData.getAcmlVol())
                .htsMktcap(kisData.getHtsMktcap())
                .w52Hgpr(kisData.getW52Hgpr())
                .build();

        return ResponseEntity.ok(result);
    }
}

// DTO로 한 번 더 감싼 이유
// 데이터베이스의 엔티티 구조는 핵심 보안 사항이라
// 이를 외부에 직접 노출하면 DB 구조가 유출될수도 있고
// 나중에 DB 설계가 바뀌면 프론트엔드 코드까지 다 고쳐야 하는 의존성 문제가 생길수 있어
// 유지보수와 보안을 위하여 DTO를 사용