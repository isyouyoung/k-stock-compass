package kopo.kstockcompass.controller;

import kopo.kstockcompass.dto.AssetHistoryDTO;
import kopo.kstockcompass.service.IAssetHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * [자산 히스토리 컨트롤러]
 *
 * 역할:
 * - 사용자의 총 자산 변동 데이터를 저장 및 조회
 * - 실시간 자산 차트(라인 그래프)용 API 제공
 *
 * 특징:
 * - 시간 기반 시계열 데이터 구조
 * - 프론트엔드 차트(Chart.js 등)와 직접 연동
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/asset-history")
public class AssetHistoryController {

    private final IAssetHistoryService assetHistoryService;

    /**
     * [자산 스냅샷 저장 API]
     *
     * 기능:
     * - 특정 시점의 유저 총자산을 DB에 저장
     *
     * 흐름:
     * 1. 프론트에서 userEmail + totalAsset 전달
     * 2. 서비스에서 AssetHistoryEntity로 저장
     *
     * 특징:
     * - 실시간 그래프를 위한 데이터 적재용 API
     * - 주기적으로(예: 3초) 호출될 수 있음
     *
     * 참고:
     * - Authorization 헤더는 현재 구조에서는 사용되지 않지만 확장 대비 포함됨
     */
    @PostMapping
    public ResponseEntity<Void> save(@RequestBody Map<String, Object> body,
                                     @RequestHeader("Authorization") String token) {

        String userEmail = (String) body.get("userEmail");
        Long totalAsset = Long.valueOf(body.get("totalAsset").toString());

        assetHistoryService.saveAssetHistory(userEmail, totalAsset);

        return ResponseEntity.ok().build();
    }

    /**
     * [자산 히스토리 조회 API]
     *
     * 기능:
     * - 특정 유저의 전체 자산 변동 기록 조회
     *
     * 용도:
     * - 자산 그래프(시간 흐름 차트) 렌더링
     *
     * 특징:
     * - 오름차순 정렬된 시계열 데이터 반환
     * - 프론트에서 바로 chart data로 사용 가능
     */
    @GetMapping("/{userEmail}")
    public ResponseEntity<List<AssetHistoryDTO>> getHistory(
            @PathVariable String userEmail) {

        return ResponseEntity.ok(
                assetHistoryService.getAssetHistory(userEmail)
        );
    }
}