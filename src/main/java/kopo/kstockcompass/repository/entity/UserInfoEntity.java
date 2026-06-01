package kopo.kstockcompass.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_INFO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoEntity {

    @Id
    @Column(name = "USER_EMAIL", length = 255)
    private String userEmail;

    @Column(name = "USER_PWD", nullable = false, length = 255)
    private String userPwd;

    @Column(name = "USER_NAME", nullable = false, length = 50)
    private String userName;

    @Column(name = "USER_PNUM", nullable = false, unique = true, length = 255)
    private String userPnum;

    @CreationTimestamp
    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt;

    // 비밀번호 변경 전용 메서드 (setter 대신 명시적 메서드 사용)
    // Entity 불변성 원칙을 지키면서 비밀번호만 선택적으로 변경 가능
    public void updatePassword(String newPwd) {
        this.userPwd = newPwd;
    }
}