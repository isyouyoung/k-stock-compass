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

/**
 * [알림 컨트롤러]
 *
 * 역할:
 * - 사용자의 지정가 알림 CRUD 처리
 * - 알림 발생 로그 조회 및 읽음 처리
 *
 * 인증:
 * - JWT 기반 인증 사용 (Authorization Header)
 * - 모든 API는 userEmail을 JWT에서 추출하여 사용
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alert")
public class AlertController {

    private final IAlertService alertService;
    private final JwtProvider jwtProvider;

    /**
     * [알림 목록 조회 API]
     *
     * 기능:
     * - 로그인한 사용자의 모든 알림 설정 조회
     *
     * 흐름:
     * 1. JWT 토큰에서 email 추출
     * 2. 해당 유저 알림 리스트 조회
     */
    @GetMapping
    public ResponseEntity<List<AlertDTO>> getAlerts(
            @RequestHeader("Authorization") String token) {

        String userEmail = getEmail(token);
        return ResponseEntity.ok(alertService.getAlerts(userEmail));
    }

    /**
     * [알림 등록 API]
     *
     * 기능:
     * - 특정 종목에 대해 목표가 알림 등록
     *
     * 특징:
     * - DTO 검증(@Valid) 적용
     * - 사용자 이메일은 JWT에서 자동 추출
     */
    @PostMapping
    public ResponseEntity<String> addAlert(
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid AlertRequestDTO dto) {

        String userEmail = getEmail(token);

        alertService.addAlert(
                userEmail,
                dto.getStockCd(),
                dto.getTargetPrice(),
                dto.getDirection()
        );

        return ResponseEntity.ok("알림이 등록되었습니다.");
    }

    /**
     * [알림 삭제 API]
     *
     * 기능:
     * - 본인이 등록한 알림만 삭제 가능
     *
     * 보안:
     * - JWT 기반 사용자 검증
     * - service layer에서 userEmail 비교 추가 검증 수행
     */
    @DeleteMapping("/{alertId}")
    public ResponseEntity<String> deleteAlert(
            @RequestHeader("Authorization") String token,
            @PathVariable Long alertId) {

        String userEmail = getEmail(token);
        alertService.deleteAlert(alertId, userEmail);

        return ResponseEntity.ok("알림이 삭제되었습니다.");
    }

    /**
     * [알림 로그 조회 API]
     *
     * 기능:
     * - 알림 조건이 충족되어 발생한 이벤트 로그 조회
     *
     * 용도:
     * - "알림 히스토리" UI 표시용
     */
    @GetMapping("/log")
    public ResponseEntity<List<AlertLogDTO>> getAlertLogs(
            @RequestHeader("Authorization") String token) {

        String userEmail = getEmail(token);
        return ResponseEntity.ok(alertService.getAlertLogs(userEmail));
    }

    /**
     * [알림 읽음 처리 API]
     *
     * 기능:
     * - 사용자가 알림 로그를 확인하면 읽음 상태(Y)로 변경
     *
     * 특징:
     * - PATCH 방식 (부분 업데이트 의미)
     * - logId 기준 상태 변경
     */
    @PatchMapping("/log/{logId}/read")
    public ResponseEntity<String> markAsRead(
            @RequestHeader("Authorization") String token,
            @PathVariable Long logId) {

        String userEmail = getEmail(token);
        alertService.markAsRead(logId, userEmail);

        return ResponseEntity.ok("읽음 처리되었습니다.");
    }

    /**
     * [JWT에서 이메일 추출 유틸 메서드]
     *
     * 역할:
     * - Authorization Header → Bearer Token 제거
     * - JWT payload에서 email 추출
     *
     * 구조:
     * Authorization: "Bearer eyJ..."
     */
    private String getEmail(String token) {
        return jwtProvider.getEmail(token.replace("Bearer ", "").trim());
    }



    /**
     * [알림 로그 삭제 API]
     * 기능: 알림 내역에서 특정 로그 삭제
     * 보안: 본인 알림 로그만 삭제 가능
     */
    @DeleteMapping("/log/{logId}")
    public ResponseEntity<String> deleteAlertLog(
            @RequestHeader("Authorization") String token,
            @PathVariable Long logId) {
        String userEmail = getEmail(token);
        alertService.deleteAlertLog(logId, userEmail);
        return ResponseEntity.ok("알림 내역이 삭제되었습니다.");
    }

    /**
     * [알림 수정 API]
     * 기능: 목표가 및 조건(이상/이하) 수정
     * 보안: 본인 알림만 수정 가능
     */
    @PutMapping("/{alertId}")
    public ResponseEntity<String> updateAlert(
            @RequestHeader("Authorization") String token,
            @PathVariable Long alertId,
            @RequestBody @Valid AlertRequestDTO dto) {
        String userEmail = getEmail(token);
        alertService.updateAlert(alertId, userEmail, dto.getTargetPrice(), dto.getDirection());
        return ResponseEntity.ok("알림이 수정되었습니다.");
    }



}