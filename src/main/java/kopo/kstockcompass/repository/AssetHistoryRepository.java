package kopo.kstockcompass.repository;

import kopo.kstockcompass.repository.entity.AssetHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetHistoryRepository extends JpaRepository<AssetHistoryEntity, Long> {
    List<AssetHistoryEntity> findByUserEmailOrderByHistIdAsc(String userEmail);
}