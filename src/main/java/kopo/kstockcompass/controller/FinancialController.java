package kopo.kstockcompass.controller;

import kopo.kstockcompass.dto.FinancialDTO;
import kopo.kstockcompass.service.IFinancialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/financial")
public class FinancialController {

    private final IFinancialService financialService;

    @GetMapping("/{stockCode}")
    public ResponseEntity<FinancialDTO> getFinancial(@PathVariable String stockCode) {
        FinancialDTO result = financialService.getFinancialData(stockCode);
        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }
}