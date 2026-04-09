package kopo.kstockcompass.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "USER_INFO")
@Getter
@Setter
@NoArgsConstructor
public class UserInfo {

    @Id
    @Column(name = "USER_EMAIL", length = 100, nullable = false)
    private String userEmail;

    @Column(name = "USER_PWD", length = 255, nullable = false)
    private String userPwd;

    @Column(name = "USER_NAME", length = 50, nullable = false)
    private String userName;

    @Column(name = "USER_PNUM", length = 20, nullable = false, unique = true)
    private String userPnum;

    @Column(name = "REG_DT", nullable = false)
    private LocalDateTime regDt;
}