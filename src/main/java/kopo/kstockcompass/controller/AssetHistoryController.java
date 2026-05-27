package kopo.kstockcompass.controller;

import kopo.kstockcompass.dto.AssetHistoryDTO;
import kopo.kstockcompass.service.IAssetHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/asset-history")
public class AssetHistoryController {

    private final IAssetHistoryService assetHistoryService;

    @PostMapping
    public ResponseEntity<Void> save(@RequestBody Map<String, Object> body,
                                     @RequestHeader("Authorization") String token) {
        String userEmail = (String) body.get("userEmail");
        Long totalAsset = Long.valueOf(body.get("totalAsset").toString());
        assetHistoryService.saveAssetHistory(userEmail, totalAsset);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userEmail}")
    public ResponseEntity<List<AssetHistoryDTO>> getHistory(@PathVariable String userEmail) {
        return ResponseEntity.ok(assetHistoryService.getAssetHistory(userEmail));
    }
}