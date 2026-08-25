package kopo.kstockcompass.repository.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_INVESTMENT_GOAL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInvestmentGoalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserInfoEntity user;

    @Column(name = "target_profit_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetProfitRate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public UserInvestmentGoalEntity(UserInfoEntity user, BigDecimal targetProfitRate) {
        this.user = user;
        this.targetProfitRate = targetProfitRate;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTargetRate(BigDecimal targetProfitRate) {
        this.targetProfitRate = targetProfitRate;
        this.updatedAt = LocalDateTime.now();
    }
}