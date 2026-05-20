package kopo.kstockcompass.repository.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ACCOUNT")
@Getter
@NoArgsConstructor
public class AccountEntity {

    @Id
    @Column(name = "USER_EMAIL", length = 100)
    private String userEmail;

    @Column(name = "CASH", precision = 20, scale = 2, nullable = false)
    private BigDecimal cash;

    @Column(name = "LOAN", precision = 20, scale = 2, nullable = false)
    private BigDecimal loan;

    @UpdateTimestamp
    @Column(name = "UPD_DT")
    private LocalDateTime updDt;

    @Builder
    public AccountEntity(String userEmail, BigDecimal cash, BigDecimal loan) {
        this.userEmail = userEmail;
        this.cash = cash;
        this.loan = loan;
    }

    public AccountEntity updateBalance(BigDecimal cash, BigDecimal loan) {
        this.cash = cash;
        this.loan = loan;
        return this;
    }
}