package kopo.kstockcompass.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ALERT_LOG")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertLogEntity {

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