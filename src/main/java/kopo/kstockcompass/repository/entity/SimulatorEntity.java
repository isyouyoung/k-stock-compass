package kopo.kstockcompass.repository.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "SIMULATOR")
@Getter
@NoArgsConstructor
public class SimulatorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SIM_ID")
    private Long simId;

    @Column(name = "USER_EMAIL", length = 100, nullable = false)
    private String userEmail;

    @Column(name = "STOCK_CD", length = 10, nullable = false)
    private String stockCd;

    @Column(name = "STOCK_NM", length = 100)
    private String stockNm;

    @Column(name = "AVG_PRICE", precision = 20, scale = 2, nullable = false)
    private BigDecimal avgPrice;

    @Column(name = "QUANTITY", nullable = false)
    private Long quantity;

    @Column(name = "TARGET_PRICE", precision = 20, scale = 2, nullable = false)
    private BigDecimal targetPrice;

    @CreationTimestamp
    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt;

    @Builder
    public SimulatorEntity(String userEmail, String stockCd, String stockNm,
                           BigDecimal avgPrice, Long quantity, BigDecimal targetPrice) {
        this.userEmail = userEmail;
        this.stockCd = stockCd;
        this.stockNm = stockNm;
        this.avgPrice = avgPrice;
        this.quantity = quantity;
        this.targetPrice = targetPrice;
    }

    public SimulatorEntity update(BigDecimal avgPrice, Long quantity, BigDecimal targetPrice) {
        this.avgPrice = avgPrice;
        this.quantity = quantity;
        this.targetPrice = targetPrice;
        return this;
    }
}