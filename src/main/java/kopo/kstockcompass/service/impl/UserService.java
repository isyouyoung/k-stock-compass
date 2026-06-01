package kopo.kstockcompass.service.impl;

import kopo.kstockcompass.config.JwtProvider;
import kopo.kstockcompass.dto.ChangePasswordRequestDTO;
import kopo.kstockcompass.dto.LoginRequestDTO;
import kopo.kstockcompass.dto.SignUpRequestDTO;
import kopo.kstockcompass.repository.entity.UserInfoEntity;
import kopo.kstockcompass.repository.UserInfoRepository;
import kopo.kstockcompass.service.IUserService;
import kopo.kstockcompass.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    // 회원가입 기능
    // 이메일, 전화번호는 AES-128 CBC 양방향 암호화 후 저장
    // 비밀번호는 BCrypt 단방향 암호화 후 저장
    @Override
    public void signUp(SignUpRequestDTO dto) throws Exception {

        // 이메일 AES-128 CBC 암호화 후 중복 체크
        String encEmail = EncryptUtil.encAES128CBC(dto.getUserEmail());

        if (userInfoRepository.existsByUserEmail(encEmail)) {
            throw new RuntimeException("이미 사용중인 이메일입니다.");
        }

        try {
            // 전화번호 AES-128 CBC 암호화
            String encPnum = EncryptUtil.encAES128CBC(dto.getUserPnum());

            UserInfoEntity user = UserInfoEntity.builder()
                    .userEmail(encEmail)                              // 암호화된 이메일 저장
                    .userPwd(passwordEncoder.encode(dto.getUserPwd())) // BCrypt 암호화
                    .userName(dto.getUserName())                       // 이름은 평문 저장
                    .userPnum(encPnum)                                // 암호화된 전화번호 저장
                    .build();

            userInfoRepository.save(user);

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 전화번호 UNIQUE 제약 위반 시
            throw new RuntimeException("이미 사용중인 전화번호입니다.");
        }
    }

    // 로그인 기능 (JWT Access + Refresh Token 발급)
    // 입력받은 이메일을 암호화하여 DB 조회
    // 토큰에는 복호화된 실제 이메일 저장
    @Override
    public Map<String, String> login(LoginRequestDTO dto) throws Exception {

        // 입력 이메일 암호화 후 DB 조회
        String encEmail = EncryptUtil.encAES128CBC(dto.getUserEmail());

        UserInfoEntity user = userInfoRepository.findByUserEmail(encEmail)
                .orElseThrow(() -> new RuntimeException("이메일이 존재하지 않습니다."));

        // 비밀번호 검증
        if (!passwordEncoder.matches(dto.getUserPwd(), user.getUserPwd())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // DB에서 꺼낸 이메일 복호화 (JWT 토큰에 실제 이메일 저장)
        String decEmail = EncryptUtil.decAES128CBC(user.getUserEmail());

        // Access Token 생성
        String accessToken = jwtProvider.createToken(decEmail);

        // Refresh Token 생성
        String refreshToken = jwtProvider.createRefreshToken(decEmail);

        // Refresh Token Redis 저장
        try {
            redisTemplate.opsForValue().set(
                    "refresh:" + decEmail,
                    refreshToken,
                    7, TimeUnit.DAYS
            );
            log.info("Refresh Token Redis 저장 완료: {}", decEmail);
        } catch (Exception e) {
            log.warn("Refresh Token Redis 저장 실패: {}", e.getMessage());
        }

        return Map.of("accessToken", accessToken, "refreshToken", refreshToken);
    }

    // 이메일 중복 확인 (암호화 후 DB 조회)
    @Override
    public boolean checkEmail(String email) throws Exception {
        String encEmail = EncryptUtil.encAES128CBC(email);
        return userInfoRepository.existsByUserEmail(encEmail);
    }

    // 아이디 찾기 (이름 + 전화번호 암호화 후 조회, 이메일 복호화 + 마스킹)
    @Override
    public String findEmail(String userName, String userPnum) throws Exception {

        // 전화번호 암호화 후 DB 조회
        String encPnum = EncryptUtil.encAES128CBC(userPnum);

        UserInfoEntity user = userInfoRepository.findByUserNameAndUserPnum(userName, encPnum)
                .orElseThrow(() -> new RuntimeException("일치하는 회원 정보가 없습니다."));

        // DB에서 꺼낸 이메일 복호화
        String email = EncryptUtil.decAES128CBC(user.getUserEmail());

        int atIndex = email.indexOf("@");
        String prefix = email.substring(0, Math.min(2, atIndex));
        return prefix + "**" + email.substring(atIndex);
    }

    // 비밀번호 초기화 (임시 비밀번호 이메일 발송)
    @Override
    public void resetPassword(String userName, String userEmail) throws Exception {

        // 이메일 암호화 후 DB 조회
        String encEmail = EncryptUtil.encAES128CBC(userEmail);

        UserInfoEntity user = userInfoRepository.findByUserEmail(encEmail)
                .filter(u -> u.getUserName().equals(userName))
                .orElseThrow(() -> new RuntimeException("입력하신 회원 정보가 일치하지 않습니다."));

        // 임시 비밀번호 생성
        String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // updatePassword 메서드로 비밀번호 변경 (setter 사용 금지)
        user.updatePassword(passwordEncoder.encode(tempPassword));
        userInfoRepository.save(user);

        // 평문 이메일로 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userEmail);
        message.setSubject("[K-Stock Compass] 임시 비밀번호 발급");
        message.setText("안녕하세요 " + userName + "님,\n\n임시 비밀번호: " + tempPassword + "\n\n로그인 후 반드시 비밀번호를 변경해주세요.");
        mailSender.send(message);
    }

    // 비밀번호 변경
    @Override
    public void changePassword(String email, ChangePasswordRequestDTO dto) throws Exception {

        // JWT에서 추출한 이메일(복호화 상태) 암호화 후 DB 조회
        String encEmail = EncryptUtil.encAES128CBC(email);

        UserInfoEntity user = userInfoRepository.findByUserEmail(encEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(dto.getCurrentPwd(), user.getUserPwd())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호가 기존과 동일한지 체크
        if (passwordEncoder.matches(dto.getNewPwd(), user.getUserPwd())) {
            throw new RuntimeException("새 비밀번호가 현재 비밀번호와 동일합니다.");
        }

        // updatePassword 메서드로 비밀번호 변경 (setter 사용 금지)
        user.updatePassword(passwordEncoder.encode(dto.getNewPwd()));
        userInfoRepository.save(user);
    }
}