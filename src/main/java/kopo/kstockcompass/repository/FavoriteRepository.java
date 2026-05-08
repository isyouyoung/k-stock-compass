package kopo.kstockcompass.repository;
import kopo.kstockcompass.repository.entity.FavoriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Long> {

    // 내 관심종목 목록 조회
    List<FavoriteEntity> findByUserEmail(String userEmail);

    // 특정 종목 관심 여부 확인 (중복 추가 방지)
    Optional<FavoriteEntity> findByUserEmailAndStockCd(String userEmail, String stockCd);

    // 관심종목 삭제
    void deleteByUserEmailAndStockCd(String userEmail, String stockCd);
}