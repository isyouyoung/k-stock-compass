package kopo.kstockcompass.controller;

import jakarta.validation.Valid;
import kopo.kstockcompass.config.JwtProvider;
import kopo.kstockcompass.dto.AlertDTO;
import kopo.kstockcompass.dto.AlertLogDTO;
import kopo.kstockcompass.dto.AlertRequestDTO;
import kopo.kstockcompass.service.IAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alert")
public class AlertController {

    private final IAlertService alertService;
    private final JwtProvider jwtProvider;

    // 알림 목록 조회
    @GetMapping
    public ResponseEntity<List<AlertDTO>> getAlerts(
            @RequestHeader("Authorization") String token) {
        String userEmail = getEmail(token);
        return ResponseEntity.ok(alertService.getAlerts(userEmail));
    }

    // 알림 등록
    @PostMapping
    public ResponseEntity<String> addAlert(
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid AlertRequestDTO dto) {
        String userEmail = getEmail(token);
        alertService.addAlert(userEmail, dto.getStockCd(), dto.getTargetPrice(), dto.getDirection());
        return ResponseEntity.ok("알림이 등록되었습니다.");
    }

    // 알림 삭제
    @DeleteMapping("/{alertId}")
    public ResponseEntity<String> deleteAlert(
            @RequestHeader("Authorization") String token,
            @PathVariable Long alertId) {
        String userEmail = getEmail(token);
        alertService.deleteAlert(alertId, userEmail);
        return ResponseEntity.ok("알림이 삭제되었습니다.");
    }

    // 알림 로그 조회
    @GetMapping("/log")
    public ResponseEntity<List<AlertLogDTO>> getAlertLogs(
            @RequestHeader("Authorization") String token) {
        String userEmail = getEmail(token);
        return ResponseEntity.ok(alertService.getAlertLogs(userEmail));
    }

    @PatchMapping("/log/{logId}/read")
    public ResponseEntity<String> markAsRead(
            @RequestHeader("Authorization") String token,
            @PathVariable Long logId) {
        String userEmail = getEmail(token);
        alertService.markAsRead(logId, userEmail);
        return ResponseEntity.ok("읽음 처리되었습니다.");
    }

    private String getEmail(String token) {
        return jwtProvider.getEmail(token.replace("Bearer ", "").trim());
    }
}