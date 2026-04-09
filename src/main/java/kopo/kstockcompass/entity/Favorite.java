package kopo.kstockcompass.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "FAVORITE")
@Getter
@Setter
@NoArgsConstructor
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FAV_ID")
    private Long favId;

    @Column(name = "USER_EMAIL", length = 100, nullable = false)
    private String userEmail;

    @Column(name = "STOCK_CD", length = 10, nullable = false)
    private String stockCd;

    @CreationTimestamp
    @Column(name = "ADD_DT", nullable = false, updatable = false)
    private LocalDateTime addDt;
}