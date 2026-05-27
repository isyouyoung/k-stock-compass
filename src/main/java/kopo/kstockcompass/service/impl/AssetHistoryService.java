package kopo.kstockcompass.service.impl;

import kopo.kstockcompass.dto.AssetHistoryDTO;
import kopo.kstockcompass.repository.AssetHistoryRepository;
import kopo.kstockcompass.repository.entity.AssetHistoryEntity;
import kopo.kstockcompass.service.IAssetHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetHistoryService implements IAssetHistoryService {

    private final AssetHistoryRepository assetHistoryRepository;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public void saveAssetHistory(String userEmail, Long totalAsset) {
        AssetHistoryEntity entity = AssetHistoryEntity.builder()
                .userEmail(userEmail)
                .totalAsset(totalAsset)
                .build();
        assetHistoryRepository.save(entity);
        log.info("자산 히스토리 저장: {} - {}원", userEmail, totalAsset);
    }

    @Override
    public List<AssetHistoryDTO> getAssetHistory(String userEmail) {
        return assetHistoryRepository.findByUserEmailOrderByRegDtAsc(userEmail)
                .stream()
                .map(h -> new AssetHistoryDTO(
                        h.getHistId(),
                        h.getUserEmail(),
                        h.getTotalAsset(),
                        h.getRegDt().format(DATE_FMT)
                ))
                .toList();
    }
}