package kopo.kstockcompass.service.impl;

import kopo.kstockcompass.config.JwtProvider;
import kopo.kstockcompass.dto.ChangePasswordRequestDTO;
import kopo.kstockcompass.dto.LoginRequestDTO;
import kopo.kstockcompass.dto.SignUpRequestDTO;
import kopo.kstockcompass.repository.entity.UserInfoEntity;
import kopo.kstockcompass.repository.UserInfoRepository;
import kopo.kstockcompass.service.IUserService;
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

    // 회원가입 기능 (이메일 중복 체크 후 사용자 저장)
    @Override
    public void signUp(SignUpRequestDTO dto) {
        if (userInfoRepository.existsByUserEmail(dto.getUserEmail())) {
            throw new RuntimeException("이미 사용중인 이메일입니다.");
        }

        try {
            // 사용자 엔티티 생성 후 DB 저장
            // 비밀번호는 BCrypt 단방향 암호화 적용 (복호화 불가)
            UserInfoEntity user = UserInfoEntity.builder()
                    .userEmail(dto.getUserEmail())
                    .userPwd(passwordEncoder.encode(dto.getUserPwd()))
                    .userName(dto.getUserName())
                    .userPnum(dto.getUserPnum())
                    .build();

            userInfoRepository.save(user);

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 전화번호 UNIQUE 제약 위반 시 (이미 가입된 전화번호)
            // DB 에러를 그대로 던지지 않고 사용자 친화적 메시지로 변환
            throw new RuntimeException("이미 사용중인 전화번호입니다.");
        }
    }

    // 로그인 기능 (JWT Access + Refresh Token 발급 구조)
    @Override
    public Map<String, String> login(LoginRequestDTO dto) {

        // 이메일 기준 사용자 조회
        UserInfoEntity user = userInfoRepository.findByUserEmail(dto.getUserEmail())
                .orElseThrow(() -> new RuntimeException("이메일이 존재하지 않습니다."));

        // 비밀번호 검증 (BCrypt matches 사용)
        if (!passwordEncoder.matches(dto.getUserPwd(), user.getUserPwd())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // Access Token 생성 (API 인증용, 단기 유효)
        String accessToken = jwtProvider.createToken(user.getUserEmail());

        // Refresh Token 생성 (재발급용, 장기 유효)
        String refreshToken = jwtProvider.createRefreshToken(user.getUserEmail());

        // Refresh Token Redis 저장 (서버측 세션 역할)
        try {
            redisTemplate.opsForValue().set(
                    "refresh:" + user.getUserEmail(),
                    refreshToken,
                    7, TimeUnit.DAYS
            );
            log.info("Refresh Token Redis 저장 완료: {}", user.getUserEmail());
        } catch (Exception e) {
            // Redis 장애 시에도 로그인은 유지되도록 예외 처리
            log.warn("Refresh Token Redis 저장 실패: {}", e.getMessage());
        }

        // 클라이언트에 Access + Refresh Token 반환
        return Map.of("accessToken", accessToken, "refreshToken", refreshToken);
    }

    // 이메일 중복 확인
    @Override
    public boolean checkEmail(String email) {
        return userInfoRepository.existsByUserEmail(email);
    }

    // 아이디 찾기 (마스킹 처리하여 일부 정보만 반환)
    @Override
    public String findEmail(String userName, String userPnum) {
        UserInfoEntity user = userInfoRepository.findByUserNameAndUserPnum(userName, userPnum)
                .orElseThrow(() -> new RuntimeException("일치하는 회원 정보가 없습니다."));

        String email = user.getUserEmail();
        int atIndex = email.indexOf("@");

        // 앞 2자리만 노출 + 나머지 마스킹 처리
        String prefix = email.substring(0, Math.min(2, atIndex));
        return prefix + "**" + email.substring(atIndex);
    }

    // 비밀번호 초기화 (임시 비밀번호 이메일 발송)
    @Override
    public void resetPassword(String userName, String userEmail) {

        // 사용자 검증 (이름 + 이메일 일치 확인)
        UserInfoEntity user = userInfoRepository.findByUserEmail(userEmail)
                .filter(u -> u.getUserName().equals(userName))
                .orElseThrow(() -> new RuntimeException("입력하신 회원 정보가 일치하지 않습니다."));

        // 임시 비밀번호 생성 (UUID 기반 8자리)
        String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 비밀번호 암호화 후 저장
        user.setUserPwd(passwordEncoder.encode(tempPassword));
        userInfoRepository.save(user);

        // 이메일 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userEmail);
        message.setSubject("[K-Stock Compass] 임시 비밀번호 발급");
        message.setText("안녕하세요 " + userName + "님,\n\n임시 비밀번호: " + tempPassword + "\n\n로그인 후 반드시 비밀번호를 변경해주세요.");
        mailSender.send(message);
    }

    // 비밀번호 변경 기능
    @Override
    public void changePassword(String email, ChangePasswordRequestDTO dto) {

        // 사용자 조회
        UserInfoEntity user = userInfoRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(dto.getCurrentPwd(), user.getUserPwd())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호가 기존과 동일한지 체크
        if (passwordEncoder.matches(dto.getNewPwd(), user.getUserPwd())) {
            throw new RuntimeException("새 비밀번호가 현재 비밀번호와 동일합니다.");
        }

        // 비밀번호 변경 (BCrypt 단방향 암호화)
        user.setUserPwd(passwordEncoder.encode(dto.getNewPwd()));
        userInfoRepository.save(user);
    }
}