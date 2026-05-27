package kopo.kstockcompass.repository.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hist_id")
    private Long histId;

    @Column(name = "user_email", length = 100, nullable = false)
    private String userEmail;

    @Column(name = "total_asset", nullable = false)
    private Long totalAsset;

    @CreationTimestamp
    @Column(name = "reg_dt", nullable = false, updatable = false)
    private LocalDateTime regDt;
}