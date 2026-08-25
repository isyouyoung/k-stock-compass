package kopo.kstockcompass.repository;

import kopo.kstockcompass.repository.entity.UserInvestmentGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserInvestmentGoalRepository extends JpaRepository<UserInvestmentGoalEntity, Long> {

    // 사용자별 목표 조회 (계정당 1개)
    Optional<UserInvestmentGoalEntity> findByUser_UserEmail(String userEmail);
}