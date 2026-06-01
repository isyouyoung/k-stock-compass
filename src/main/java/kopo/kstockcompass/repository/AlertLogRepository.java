package kopo.kstockcompass.repository;

import kopo.kstockcompass.repository.entity.AlertLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AlertLogRepository extends JpaRepository<AlertLogEntity, Long> {

    // 알림 ID로 로그 조회
    List<AlertLogEntity> findByAlertId(Long alertId);

    // 알림 ID 목록으로 로그 조회 (내 알림 내역 전체)
    List<AlertLogEntity> findByAlertIdIn(List<Long> alertIds);

    @Transactional
    void deleteByAlertIdIn(List<Long> alertIds);
}