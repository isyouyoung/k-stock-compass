package kopo.kstockcompass.repository.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TRADE_LOG")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserInfoEntity user;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName;

    @Column(name = "buy_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal buyPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public TradeLogEntity(UserInfoEntity user, String stockCode, String stockName,
                          BigDecimal buyPrice, Integer quantity) {
        this.user = user;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.buyPrice = buyPrice;
        this.quantity = quantity;
        this.createdAt = LocalDateTime.now();
    }

    // Dirty checking용 명시적 상태 변경 메서드
    public void updateBuyInfo(BigDecimal buyPrice, Integer quantity) {
        this.buyPrice = buyPrice;
        this.quantity = quantity;
    }
}