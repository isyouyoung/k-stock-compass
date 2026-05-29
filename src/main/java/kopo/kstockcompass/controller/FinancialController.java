package kopo.kstockcompass.controller;

import kopo.kstockcompass.dto.FinancialDTO;
import kopo.kstockcompass.service.IFinancialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * [재무 데이터 컨트롤러]
 *
 * 역할:
 * - 특정 종목의 재무제표 데이터 조회 API 제공
 * - DART 기반 재무 데이터 전달
 *
 * 특징:
 * - AI 분석 / 투자 판단 로직의 핵심 입력 데이터 제공
 * - FinancialService → DART API 연동 결과 반환
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/financial")
public class FinancialController {

    private final IFinancialService financialService;

    /**
     * [재무 데이터 조회 API]
     *
     * 기능:
     * - stockCode 기준으로 재무 데이터 조회
     *
     * 흐름:
     * 1. Service에서 DART API 호출
     * 2. FinancialDTO로 변환
     * 3. 프론트 또는 AI 서비스에 전달
     *
     * 응답 정책:
     * - 데이터 없으면 204 No Content 반환
     * - 데이터 있으면 200 OK + JSON 반환
     */
    @GetMapping("/{stockCode}")
    public ResponseEntity<FinancialDTO> getFinancial(
            @PathVariable String stockCode) {

        FinancialDTO result = financialService.getFinancialData(stockCode);

        if (result == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(result);
    }
}