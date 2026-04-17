package kopo.kstockcompass.service;

import kopo.kstockcompass.config.JwtProvider;
import kopo.kstockcompass.dto.LoginRequestDTO;
import kopo.kstockcompass.dto.SignUpRequestDTO;
import kopo.kstockcompass.entity.UserInfo;
import kopo.kstockcompass.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider; // JWT 추가!
    private final JavaMailSender mailSender;

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

    // 비밀번호 변경 (임시 비밀번호 발송)
    public void resetPassword(String userName, String userEmail) {

        UserInfo user = userInfoRepository.findByUserEmail(userEmail)
                .filter(u -> u.getUserName().equals(userName))
                .orElseThrow(() -> new RuntimeException("입력하신 회원 정보가 일치하지 않습니다."));

        // 임시 비밀번호 생성 (하이픈 제거 8자리)
        String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 암호화 후 DB 저장
        user.setUserPwd(passwordEncoder.encode(tempPassword));
        userInfoRepository.save(user);

        // 이메일 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userEmail);
        message.setSubject("[K-Stock Compass] 임시 비밀번호 발급");
        message.setText("안녕하세요 " + userName + "님,\n\n임시 비밀번호: " + tempPassword + "\n\n로그인 후 반드시 비밀번호를 변경해주세요.");
        mailSender.send(message);
    }


}