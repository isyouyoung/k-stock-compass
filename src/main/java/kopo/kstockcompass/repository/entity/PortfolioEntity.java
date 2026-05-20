package kopo.kstockcompass.repository.entity;
// 신규 기능 추가

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PORTFOLIO")
@Getter
@NoArgsConstructor
public class PortfolioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PORT_ID")
    private Long portId;

    @Column(name = "USER_EMAIL", length = 100, nullable = false)
    private String userEmail;

    @Column(name = "STOCK_CD", length = 10, nullable = false)
    private String stockCd;

    @Column(name = "AVG_PRICE", precision = 20, scale = 2, nullable = false)
    private BigDecimal avgPrice;

    @Column(name = "QUANTITY", nullable = false)
    private Long quantity;

    @CreationTimestamp
    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt;

    @Builder
    public PortfolioEntity(String userEmail, String stockCd, BigDecimal avgPrice, Long quantity) {
        this.userEmail = userEmail;
        this.stockCd = stockCd;
        this.avgPrice = avgPrice;
        this.quantity = quantity;
    }
}