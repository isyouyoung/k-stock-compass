package kopo.kstockcompass.controller;

import kopo.kstockcompass.config.JwtProvider;
import kopo.kstockcompass.dto.AccountDTO;
import kopo.kstockcompass.dto.PortfolioDTO;
import kopo.kstockcompass.service.IPortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final IPortfolioService portfolioService;
    private final JwtProvider jwtProvider;

    private String getEmail(String token) {
        return jwtProvider.getEmail(token.replace("Bearer ", ""));
    }

    // 보유종목 조회
    @GetMapping
    public ResponseEntity<List<PortfolioDTO>> getPortfolio(
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(portfolioService.getPortfolio(getEmail(token)));
    }

    // 보유종목 추가
    @PostMapping
    public ResponseEntity<String> addPortfolio(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> body) {
        String email = getEmail(token);
        String stockCd = (String) body.get("stockCd");
        Long avgPrice = Long.valueOf(body.get("avgPrice").toString());
        Long quantity = Long.valueOf(body.get("quantity").toString());
        portfolioService.addPortfolio(email, stockCd, avgPrice, quantity);
        return ResponseEntity.ok("추가되었습니다.");
    }

    // 보유종목 삭제
    @DeleteMapping("/{portId}")
    public ResponseEntity<String> deletePortfolio(
            @RequestHeader("Authorization") String token,
            @PathVariable Long portId) {
        portfolioService.deletePortfolio(portId, getEmail(token));
        return ResponseEntity.ok("삭제되었습니다.");
    }

    // 예수금/대출금 조회
    @GetMapping("/account")
    public ResponseEntity<AccountDTO> getAccount(
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(portfolioService.getAccount(getEmail(token)));
    }

    // 예수금/대출금 수정
    @PutMapping("/account")
    public ResponseEntity<String> updateAccount(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> body) {
        String email = getEmail(token);
        Long cash = Long.valueOf(body.get("cash").toString());
        Long loan = Long.valueOf(body.get("loan").toString());
        portfolioService.updateAccount(email, cash, loan);
        return ResponseEntity.ok("수정되었습니다.");
    }
}