package kopo.kstockcompass.controller;

import kopo.kstockcompass.config.JwtProvider;
import kopo.kstockcompass.dto.SimulatorDTO;
import kopo.kstockcompass.service.ISimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/simulator")
public class SimulatorController {

    private final ISimulatorService simulatorService;
    private final JwtProvider jwtProvider;

    private String getEmail(String token) {
        return jwtProvider.getEmail(token.replace("Bearer ", ""));
    }

    // 시뮬레이터 목록 조회
    @GetMapping
    public ResponseEntity<List<SimulatorDTO>> getSimulator(
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(simulatorService.getSimulator(getEmail(token)));
    }

    // 시뮬레이터 종목 추가
    @PostMapping
    public ResponseEntity<String> addSimulator(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> body) {
        String email = getEmail(token);
        String stockCd = (String) body.get("stockCd");
        String stockNm = (String) body.get("stockNm");
        Long avgPrice = Long.valueOf(body.get("avgPrice").toString());
        Long quantity = Long.valueOf(body.get("quantity").toString());
        Long targetPrice = Long.valueOf(body.get("targetPrice").toString());
        simulatorService.addSimulator(email, stockCd, stockNm, avgPrice, quantity, targetPrice);
        return ResponseEntity.ok("추가되었습니다.");
    }

    // 시뮬레이터 종목 수정
    @PutMapping("/{simId}")
    public ResponseEntity<String> updateSimulator(
            @RequestHeader("Authorization") String token,
            @PathVariable Long simId,
            @RequestBody Map<String, Object> body) {
        String email = getEmail(token);
        Long avgPrice = Long.valueOf(body.get("avgPrice").toString());
        Long quantity = Long.valueOf(body.get("quantity").toString());
        Long targetPrice = Long.valueOf(body.get("targetPrice").toString());
        simulatorService.updateSimulator(simId, email, avgPrice, quantity, targetPrice);
        return ResponseEntity.ok("수정되었습니다.");
    }

    // 시뮬레이터 종목 삭제
    @DeleteMapping("/{simId}")
    public ResponseEntity<String> deleteSimulator(
            @RequestHeader("Authorization") String token,
            @PathVariable Long simId) {
        simulatorService.deleteSimulator(simId, getEmail(token));
        return ResponseEntity.ok("삭제되었습니다.");
    }
}