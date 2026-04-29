package kopo.kstockcompass.repository;

import kopo.kstockcompass.repository.entity.UserInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfoEntity, String> {

    // 이메일 중복 체크 (회원가입)
    boolean existsByUserEmail(String userEmail);

    // 로그인용 (이메일로 조회)
    Optional<UserInfoEntity> findByUserEmail(String userEmail);

    // 아이디 찾기 (이름 + 전화번호로 조회)
    Optional<UserInfoEntity> findByUserNameAndUserPnum(String userName, String userPnum);
}