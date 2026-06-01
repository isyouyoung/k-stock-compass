package kopo.kstockcompass.repository;

import kopo.kstockcompass.repository.entity.PortfolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PortfolioRepository extends JpaRepository<PortfolioEntity, Long> {
    List<PortfolioEntity> findByUserEmail(String userEmail);

    @Transactional
    void deleteByPortIdAndUserEmail(Long portId, String userEmail);

    @Transactional
    void deleteByUserEmail(String userEmail);
}