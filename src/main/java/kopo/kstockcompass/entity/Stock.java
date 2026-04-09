package kopo.kstockcompass.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp; // 추가됨
import java.time.LocalDateTime;

@Entity
@Table(name = "STOCK")
@Getter
@Setter
@NoArgsConstructor
public class Stock {

    @Id
    @Column(name = "STOCK_CD", length = 10, nullable = false)
    private String stockCd;

    @Column(name = "STOCK_NM", length = 100, nullable = false)
    private String stockNm;

    @Column(name = "MKT_TYPE", length = 10, nullable = false)
    private String mktType;

    @CreationTimestamp // 데이터 저장 시 현재 시간 자동 입력
    @Column(name = "REG_DT", nullable = false, updatable = false) // 수정 방지
    private LocalDateTime regDt;
}