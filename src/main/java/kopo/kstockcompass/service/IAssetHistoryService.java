package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.AssetHistoryDTO;
import java.util.List;

public interface IAssetHistoryService {
    void saveAssetHistory(String userEmail, Long totalAsset);
    List<AssetHistoryDTO> getAssetHistory(String userEmail);
}