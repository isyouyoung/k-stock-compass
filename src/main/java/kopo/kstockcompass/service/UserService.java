package kopo.kstockcompass.service;

import kopo.kstockcompass.config.JwtProvider;
import kopo.kstockcompass.dto.LoginRequestDTO;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider; // JWT 추가!

    // 회원가입
    public void signUp(SignUpRequestDTO dto) {

        if (userInfoRepository.existsByUserEmail(dto.getUserEmail())) {
            throw new RuntimeException("이미 사용중인 이메일입니다.");
        }

        UserInfo user = new UserInfo();
        user.setUserEmail(dto.getUserEmail());
        user.setUserPwd(passwordEncoder.encode(dto.getUserPwd()));
        user.setUserName(dto.getUserName());
        user.setUserPnum(dto.getUserPnum());

        userInfoRepository.save(user);
    }

    // 로그인 → JWT 토큰 반환
    public String login(LoginRequestDTO dto) {

        UserInfo user = userInfoRepository.findByUserEmail(dto.getUserEmail())
                .orElseThrow(() -> new RuntimeException("이메일이 존재하지 않습니다."));

        if (!passwordEncoder.matches(dto.getUserPwd(), user.getUserPwd())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // 로그인 성공 → JWT 토큰 발급!
        return jwtProvider.createToken(user.getUserEmail());
    }

    // 이메일 중복 체크
    public boolean checkEmail(String email) {
        return userInfoRepository.existsByUserEmail(email);
    }

    // 아이디 찾기
    public String findEmail(String userName, String userPnum) {

        UserInfo user = userInfoRepository.findByUserNameAndUserPnum(userName, userPnum)
                .orElseThrow(() -> new RuntimeException("일치하는 회원 정보가 없습니다."));

        String email = user.getUserEmail();
        int atIndex = email.indexOf("@");
        String prefix = email.substring(0, Math.min(2, atIndex));
        String masked = prefix + "**" + email.substring(atIndex);

        return masked;
    }


}