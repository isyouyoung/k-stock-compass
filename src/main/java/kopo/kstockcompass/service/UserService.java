package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.SignUpRequestDTO;
import kopo.kstockcompass.entity.UserInfo;
import kopo.kstockcompass.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder; // 인터페이스로 주입!

    // 회원가입
    public void signUp(SignUpRequestDTO dto) {

        // 이메일 중복 체크
        if (userInfoRepository.existsByUserEmail(dto.getUserEmail())) {
            throw new RuntimeException("이미 사용중인 이메일입니다.");
        }

        // Entity 생성 + 비밀번호 암호화
        UserInfo user = new UserInfo();
        user.setUserEmail(dto.getUserEmail());
        user.setUserPwd(passwordEncoder.encode(dto.getUserPwd()));
        user.setUserName(dto.getUserName());
        user.setUserPnum(dto.getUserPnum());

        // DB 저장
        userInfoRepository.save(user);
    }
}