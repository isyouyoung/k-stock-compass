package kopo.kstockcompass.service.impl;

import kopo.kstockcompass.config.JwtProvider;
import kopo.kstockcompass.dto.ChangePasswordRequestDTO;
import kopo.kstockcompass.dto.LoginRequestDTO;
import kopo.kstockcompass.dto.SignUpRequestDTO;
import kopo.kstockcompass.repository.entity.UserInfoEntity;
import kopo.kstockcompass.repository.UserInfoRepository;
import kopo.kstockcompass.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider; // JWT 추가!
    private final JavaMailSender mailSender;

    // 회원가입
    @Override
    public void signUp(SignUpRequestDTO dto) {

        if (userInfoRepository.existsByUserEmail(dto.getUserEmail())) {
            throw new RuntimeException("이미 사용중인 이메일입니다.");
        }

        // 3차 평가 반영 예정: AES-128 CBC 식별정보 암호화
        // 현재는 평문 저장 중이며, 3차 평가 전까지 이메일·전화번호에
        // AES-128 CBC 양방향 암호화를 적용할 예정임
        // 이메일(식별정보)과 전화번호(식별정보)는 복호화가 필요하므로
        // 단방향인 BCrypt가 아닌 양방향 AES 암호화를 사용해야 함
        UserInfoEntity user = UserInfoEntity.builder()
                .userEmail(dto.getUserEmail())
                .userPwd(passwordEncoder.encode(dto.getUserPwd()))
                .userName(dto.getUserName())
                .userPnum(dto.getUserPnum())
                .build();

        userInfoRepository.save(user);
    }

    // 로그인 → JWT 토큰 반환
    @Override
    public String login(LoginRequestDTO dto) {

        UserInfoEntity user = userInfoRepository.findByUserEmail(dto.getUserEmail())
                .orElseThrow(() -> new RuntimeException("이메일이 존재하지 않습니다."));

        if (!passwordEncoder.matches(dto.getUserPwd(), user.getUserPwd())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // 로그인 성공 → JWT 토큰 발급!
        return jwtProvider.createToken(user.getUserEmail());
    }

    // 이메일 중복 체크
    @Override
    public boolean checkEmail(String email) {
        return userInfoRepository.existsByUserEmail(email);
    }

    // 아이디 찾기
    @Override
    public String findEmail(String userName, String userPnum) {

        UserInfoEntity user = userInfoRepository.findByUserNameAndUserPnum(userName, userPnum)
                .orElseThrow(() -> new RuntimeException("일치하는 회원 정보가 없습니다."));

        String email = user.getUserEmail();
        int atIndex = email.indexOf("@");
        String prefix = email.substring(0, Math.min(2, atIndex));
        String masked = prefix + "**" + email.substring(atIndex);

        return masked;
    }

    // 비밀번호 변경 (임시 비밀번호 발송)
    @Override
    public void resetPassword(String userName, String userEmail) {

        UserInfoEntity user = userInfoRepository.findByUserEmail(userEmail)
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

    @Override
    public void changePassword(String email, ChangePasswordRequestDTO dto) {
        UserInfoEntity user = userInfoRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(dto.getCurrentPwd(), user.getUserPwd())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        // [비밀번호 단방향 암호화 - BCrypt]
        // BCrypt는 단방향 해시 암호화로 복호화가 불가능함
        // passwordEncoder.encode() : 평문 비밀번호를 BCrypt로 암호화하여 DB에 저장
        user.setUserPwd(passwordEncoder.encode(dto.getNewPwd()));
        userInfoRepository.save(user);
    }


}