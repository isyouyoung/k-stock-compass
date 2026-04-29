package kopo.kstockcompass.repository;

import kopo.kstockcompass.repository.entity.AlertLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertLogRepository extends JpaRepository<AlertLogEntity, Long> {
}