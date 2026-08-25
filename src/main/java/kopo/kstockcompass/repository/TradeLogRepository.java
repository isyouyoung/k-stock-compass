package kopo.kstockcompass.repository;

import kopo.kstockcompass.repository.entity.TradeLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TradeLogRepository extends JpaRepository<TradeLogEntity, Long> {

    // 사용자별 전체 매매일지 조회 (최신순)
    List<TradeLogEntity> findByUser_UserEmailOrderByCreatedAtDesc(String userEmail);

    // 사용자별 특정 매매일지 조회 (수정/삭제 시 본인 확인용)
    Optional<TradeLogEntity> findByIdAndUser_UserEmail(Long id, String userEmail);
}