package kopo.kstockcompass.repository;

import kopo.kstockcompass.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfo, String> {

    // 이메일 중복 체크 (회원가입)
    boolean existsByUserEmail(String userEmail);

    // 로그인용 (이메일로 조회)
    Optional<UserInfo> findByUserEmail(String userEmail);

    // 아이디 찾기 (이름 + 전화번호로 조회)
    Optional<UserInfo> findByUserNameAndUserPnum(String userName, String userPnum);
}