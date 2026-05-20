package kopo.kstockcompass.repository;

import kopo.kstockcompass.repository.entity.SimulatorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SimulatorRepository extends JpaRepository<SimulatorEntity, Long> {
    List<SimulatorEntity> findByUserEmail(String userEmail);

    @Transactional
    void deleteBySimIdAndUserEmail(Long simId, String userEmail);
}