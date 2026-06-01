package kopo.kstockcompass.repository;

import kopo.kstockcompass.repository.entity.AlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AlertRepository extends JpaRepository<AlertEntity, Long> {

    // 내 알림 목록 조회
    List<AlertEntity> findByUserEmail(String userEmail);

    // 특정 종목 알림 여부 확인
    boolean existsByUserEmailAndStockCd(String userEmail, String stockCd);

    // 알림 삭제
    void deleteByAlertIdAndUserEmail(Long alertId, String userEmail);

    @Transactional
    void deleteByUserEmail(String userEmail);
}