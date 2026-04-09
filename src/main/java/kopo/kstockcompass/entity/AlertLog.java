package kopo.kstockcompass.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ALERT_LOG")
@Getter
@Setter
@NoArgsConstructor
public class AlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOG_ID")
    private Long logId;

    @Column(name = "ALERT_ID", nullable = false)
    private Long alertId;

    @Column(name = "MSG", nullable = false, columnDefinition = "TEXT")
    private String msg;

    @CreationTimestamp
    @Column(name = "SEND_DT", nullable = false, updatable = false)
    private LocalDateTime sendDt;

    @Column(name = "IS_READ", length = 1, nullable = false)
    private String isRead = "N";
}