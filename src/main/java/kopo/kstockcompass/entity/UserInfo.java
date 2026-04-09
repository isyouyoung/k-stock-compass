package kopo.kstockcompass.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp; // 추가
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_INFO")
@Getter @Setter @NoArgsConstructor
public class UserInfo {
    @Id
    @Column(name = "USER_EMAIL", length = 100)
    private String userEmail;

    @Column(name = "USER_PWD", nullable = false, length = 255)
    private String userPwd;

    @Column(name = "USER_NAME", nullable = false, length = 50)
    private String userName;

    @Column(name = "USER_PNUM", nullable = false, unique = true, length = 20)
    private String userPnum;

    @CreationTimestamp // 자동으로 현재 시간 입력
    @Column(name = "REG_DT", nullable = false, updatable = false) // 수정 방지
    private LocalDateTime regDt;
}