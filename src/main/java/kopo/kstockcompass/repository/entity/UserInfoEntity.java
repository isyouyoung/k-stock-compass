package kopo.kstockcompass.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp; // 추가
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_INFO")
// 교수님 PPT6장 9페이지 참고
@Getter @Setter @NoArgsConstructor
@AllArgsConstructor
@Builder // 교수님 PPT6장 8페이지 참고
public class UserInfoEntity {
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