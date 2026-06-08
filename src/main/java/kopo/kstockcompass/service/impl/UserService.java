package kopo.kstockcompass.service.impl;

import kopo.kstockcompass.config.JwtProvider;
import kopo.kstockcompass.dto.ChangePasswordRequestDTO;
import kopo.kstockcompass.dto.LoginRequestDTO;
import kopo.kstockcompass.dto.SignUpRequestDTO;
import kopo.kstockcompass.repository.entity.AlertEntity;
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
import org.springframework.transaction.annotation.Transactional;
import kopo.kstockcompass.repository.*;
import java.util.List;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// @Slf4j: log.info(), log.warn() 등 로그 출력을 위한 Lombok 어노테이션
// @Service: 이 클래스가 비즈니스 로직을 담당하는 서비스 계층임을 스프링에 알림
// @RequiredArgsConstructor: final 필드들을 자동으로 생성자 주입
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    // ── 의존성 주입 ──────────────────────────────────────────────
    private final UserInfoRepository userInfoRepository;    // 회원 정보 DB 접근
    private final PasswordEncoder passwordEncoder;          // BCrypt 비밀번호 암호화 도구
    private final JwtProvider jwtProvider;                  // JWT 토큰 생성/검증 도구
    private final JavaMailSender mailSender;                // 이메일 발송 도구
    private final StringRedisTemplate redisTemplate;        // Redis 접근 도구 (Refresh Token 저장/조회용)
    private final AlertRepository alertRepository;          // 알림 설정 DB 접근
    private final AlertLogRepository alertLogRepository;    // 알림 로그 DB 접근
    private final FavoriteRepository favoriteRepository;    // 관심종목 DB 접근
    private final PortfolioRepository portfolioRepository;  // 포트폴리오 DB 접근
    private final AccountRepository accountRepository;      // 계좌 정보 DB 접근
    private final SimulatorRepository simulatorRepository;  // 시뮬레이터 DB 접근
    private final AssetHistoryRepository assetHistoryRepository; // 자산 히스토리 DB 접근

    /**
     * [회원가입]
     * 보안 처리:
     * - 이메일, 전화번호: AES-128 CBC 양방향 암호화 후 DB 저장
     *   → 복호화가 필요한 데이터(아이디 찾기 등)이므로 양방향 암호화 사용
     * - 비밀번호: BCrypt 단방향 암호화 후 DB 저장
     *   → 복호화가 불필요하고 보안이 중요하므로 단방향 암호화 사용
     */
    @Override
    public void signUp(SignUpRequestDTO dto) throws Exception {

        // 입력받은 이메일을 AES-128 CBC로 암호화하여 DB에 같은 형태로 존재하는지 중복 체크
        // DB에는 암호화된 이메일이 저장되어 있으므로 암호화 후 비교해야 함
        String encEmail = EncryptUtil.encAES128CBC(dto.getUserEmail());

        if (userInfoRepository.existsByUserEmail(encEmail)) {
            throw new RuntimeException("이미 사용중인 이메일입니다.");
        }

        try {
            // 전화번호 AES-128 CBC 암호화
            String encPnum = EncryptUtil.encAES128CBC(dto.getUserPnum());

            // Entity 빌더 패턴으로 생성 (@Setter 사용 금지 - 불변성 원칙)
            UserInfoEntity user = UserInfoEntity.builder()
                    .userEmail(encEmail)                               // 암호화된 이메일 저장
                    .userPwd(passwordEncoder.encode(dto.getUserPwd())) // BCrypt로 비밀번호 단방향 암호화
                    .userName(dto.getUserName())                        // 이름은 검색 필요 없으므로 평문 저장
                    .userPnum(encPnum)                                 // 암호화된 전화번호 저장
                    .build();

            userInfoRepository.save(user); // DB에 저장

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 전화번호 컬럼에 UNIQUE 제약조건이 걸려있어서 중복 시 예외 발생
            throw new RuntimeException("이미 사용중인 전화번호입니다.");
        }
    }

    /**
     * [로그인 - JWT Access Token + Refresh Token 발급]
     *
     * 흐름:
     * 1. 입력 이메일 암호화 → DB 조회 (DB엔 암호화된 이메일 저장)
     * 2. BCrypt로 비밀번호 검증
     * 3. DB 이메일 복호화 → JWT에 실제 이메일 저장
     * 4. Access Token(10분) + Refresh Token(7일) 발급
     * 5. Refresh Token을 Redis에 저장 (key: "refresh:{이메일}", TTL: 7일)
     *
     * Redis를 쓰는 이유:
     * - Refresh Token은 7일짜리 장기 토큰이라 탈취 시 위험
     * - Redis에 저장해두면 서버에서 강제 무효화(로그아웃, 탈취 감지)가 가능
     * - DB 대신 Redis를 쓰는 이유: 빠른 조회 속도 + TTL 자동 만료 기능
     */
    @Override
    public Map<String, String> login(LoginRequestDTO dto) throws Exception {

        // 1. 입력 이메일 암호화 후 DB 조회 (DB에는 암호화된 이메일이 저장되어 있음)
        String encEmail = EncryptUtil.encAES128CBC(dto.getUserEmail());

        UserInfoEntity user = userInfoRepository.findByUserEmail(encEmail)
                .orElseThrow(() -> new RuntimeException("이메일이 존재하지 않습니다."));

        // 2. BCrypt 비밀번호 검증 (입력값과 DB 암호화값 비교)
        // BCrypt는 단방향이므로 복호화 불가 → matches()로 비교
        if (!passwordEncoder.matches(dto.getUserPwd(), user.getUserPwd())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // 3. DB에서 꺼낸 암호화된 이메일을 복호화 (JWT 토큰에는 실제 이메일 저장)
        String decEmail = EncryptUtil.decAES128CBC(user.getUserEmail());

        // 4. Access Token 생성 (만료시간: 10분)
        String accessToken = jwtProvider.createToken(decEmail);

        // 5. Refresh Token 생성 (만료시간: 7일)
        String refreshToken = jwtProvider.createRefreshToken(decEmail);

        // 6. Refresh Token을 Redis에 저장
        // key: "refresh:{이메일}", value: refreshToken 문자열, TTL: 7일
        // Redis TTL을 7일로 설정하면 7일 후 자동으로 삭제됨 (DB보다 편리)
        // try-catch: Redis 연결 실패 시 로그인 자체가 막히면 안 되므로 예외를 삼킴
        try {
            redisTemplate.opsForValue().set(
                    "refresh:" + decEmail, // Redis key (이메일별로 하나만 유지)
                    refreshToken,          // Redis value (Refresh Token 문자열)
                    7, TimeUnit.DAYS       // TTL 7일 (7일 후 자동 삭제)
            );
            log.info("Refresh Token Redis 저장 완료: {}", decEmail);
        } catch (Exception e) {
            // Redis 연결 실패 시 로그만 남기고 로그인은 정상 진행
            // 단, Refresh Token 재발급 기능은 Redis 없이 동작 불가
            log.warn("Refresh Token Redis 저장 실패: {}", e.getMessage());
        }

        // 7. Access Token + Refresh Token 둘 다 프론트로 반환
        return Map.of("accessToken", accessToken, "refreshToken", refreshToken);
    }

    /**
     * [이메일 중복 확인]
     * 입력받은 이메일을 암호화 후 DB 조회
     * DB에는 암호화된 이메일이 저장되어 있으므로 반드시 암호화 후 비교
     */
    @Override
    public boolean checkEmail(String email) throws Exception {
        String encEmail = EncryptUtil.encAES128CBC(email);
        return userInfoRepository.existsByUserEmail(encEmail);
    }

    /**
     * [아이디 찾기]
     * 이름 + 전화번호로 이메일 조회 후 마스킹 처리하여 반환
     * 전화번호는 암호화 후 DB 조회, 이메일은 복호화 후 마스킹
     */
    @Override
    public String findEmail(String userName, String userPnum) throws Exception {

        // 전화번호 암호화 후 DB 조회 (DB에 암호화된 전화번호가 저장되어 있음)
        String encPnum = EncryptUtil.encAES128CBC(userPnum);

        UserInfoEntity user = userInfoRepository.findByUserNameAndUserPnum(userName, encPnum)
                .orElseThrow(() -> new RuntimeException("일치하는 회원 정보가 없습니다."));

        // DB에서 꺼낸 암호화된 이메일 복호화
        String email = EncryptUtil.decAES128CBC(user.getUserEmail());

        // 이메일 앞 2글자만 보여주고 나머지 마스킹 처리 (예: ab**@example.com)
        int atIndex = email.indexOf("@");
        String prefix = email.substring(0, Math.min(2, atIndex));
        return prefix + "**" + email.substring(atIndex);
    }

    /**
     * [비밀번호 초기화 - 임시 비밀번호 이메일 발송]
     * 이름 + 이메일로 회원 조회 후 임시 비밀번호 생성하여 이메일 발송
     * 임시 비밀번호는 BCrypt 암호화 후 DB에 저장
     */
    @Override
    public void resetPassword(String userName, String userEmail) throws Exception {

        // 이메일 암호화 후 DB 조회
        String encEmail = EncryptUtil.encAES128CBC(userEmail);

        UserInfoEntity user = userInfoRepository.findByUserEmail(encEmail)
                .filter(u -> u.getUserName().equals(userName))
                .orElseThrow(() -> new RuntimeException("입력하신 회원 정보가 일치하지 않습니다."));

        // UUID로 랜덤 임시 비밀번호 생성 (8자리)
        String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // setter 사용 금지 원칙 → Entity 내부 메서드로 비밀번호 변경
        user.updatePassword(passwordEncoder.encode(tempPassword));
        userInfoRepository.save(user);

        // 발송은 복호화된 평문 이메일로 발송 (암호화된 이메일로 발송하면 안 됨)
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userEmail);
        message.setSubject("[K-Stock Compass] 임시 비밀번호 발급");
        message.setText("안녕하세요 " + userName + "님,\n\n임시 비밀번호: " + tempPassword + "\n\n로그인 후 반드시 비밀번호를 변경해주세요.");
        mailSender.send(message);
    }

    /**
     * [비밀번호 변경]
     * JWT에서 추출한 이메일(복호화 상태)을 다시 암호화하여 DB 조회
     * 현재 비밀번호 검증 후 새 비밀번호로 변경
     */
    @Override
    public void changePassword(String email, ChangePasswordRequestDTO dto) throws Exception {

        // JWT에서 추출한 이메일은 복호화 상태 → 다시 암호화하여 DB 조회
        String encEmail = EncryptUtil.encAES128CBC(email);

        UserInfoEntity user = userInfoRepository.findByUserEmail(encEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 현재 비밀번호 검증 (BCrypt matches로 비교)
        if (!passwordEncoder.matches(dto.getCurrentPwd(), user.getUserPwd())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호가 기존과 동일한지 체크 (동일하면 변경 의미 없음)
        if (passwordEncoder.matches(dto.getNewPwd(), user.getUserPwd())) {
            throw new RuntimeException("새 비밀번호가 현재 비밀번호와 동일합니다.");
        }

        // setter 사용 금지 원칙 → Entity 내부 메서드로 비밀번호 변경
        user.updatePassword(passwordEncoder.encode(dto.getNewPwd()));
        userInfoRepository.save(user);
    }

    /**
     * [회원 탈퇴]
     * 회원 관련 데이터를 외래키 순서대로 삭제
     * @Transactional: 중간에 실패하면 전체 롤백 보장
     *
     * 삭제 순서 (외래키 제약조건 때문에 순서 중요):
     * 알림 로그 → 알림 → 관심종목 → 포트폴리오 → 계좌 → 시뮬레이터 → 자산히스토리 → 회원정보
     */
    @Override
    @Transactional
    public void deleteUser(String email) throws Exception {

        // JWT에서 추출한 복호화 이메일 → 암호화하여 DB 조회
        String encEmail = EncryptUtil.encAES128CBC(email);

        // 1. 내 알림 목록 조회 (알림 로그 삭제를 위해 alertId 목록 필요)
        List<AlertEntity> alerts = alertRepository.findByUserEmail(encEmail);
        if (!alerts.isEmpty()) {
            List<Long> alertIds = alerts.stream()
                    .map(AlertEntity::getAlertId)
                    .toList();
            // 2. 알림 로그 삭제 (alert_log가 alert를 외래키로 참조하므로 먼저 삭제)
            alertLogRepository.deleteByAlertIdIn(alertIds);
            // 3. 알림 삭제
            alertRepository.deleteByUserEmail(encEmail);
        }

        // 4. 관심종목 삭제
        favoriteRepository.deleteByUserEmail(encEmail);

        // 5. 포트폴리오 삭제
        portfolioRepository.deleteByUserEmail(encEmail);

        // 6. 계좌 삭제
        accountRepository.deleteById(encEmail);

        // 7. 시뮬레이터 삭제
        simulatorRepository.deleteByUserEmail(encEmail);

        // 8. 자산 히스토리 삭제
        assetHistoryRepository.deleteByUserEmail(encEmail);

        // 9. 회원 정보 삭제 (모든 연관 데이터 삭제 후 마지막에 삭제)
        userInfoRepository.deleteById(encEmail);

        log.info("회원 탈퇴 완료: {}", email);
    }

    /**
     * [Access Token 재발급]
     *
     * 흐름:
     * 1. Refresh Token JWT 유효성 검사 (만료/변조 여부)
     * 2. Refresh Token에서 이메일 추출
     * 3. Redis에 저장된 Refresh Token과 비교 → 탈취 방지 핵심!
     *    - Redis에 없으면: 만료되었거나 로그아웃된 상태
     *    - Redis 값과 다르면: 이미 재발급된 토큰이거나 탈취된 토큰
     * 4. 일치하면 새 Access Token 발급하여 반환
     *
     * Redis를 쓰는 이유:
     * - 서버가 발급한 Refresh Token이 맞는지 서버 측에서 검증 가능
     * - DB 대신 Redis: 빠른 조회 + TTL 자동 만료 관리
     */
    @Override
    public String refreshAccessToken(String refreshToken) throws Exception {

        // 1. Refresh Token 유효성 검사 (만료되었거나 변조된 토큰이면 예외 발생)
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 Refresh Token입니다.");
        }

        // 2. Refresh Token에서 이메일 추출
        String email = jwtProvider.getEmail(refreshToken);

        // 3. Redis에서 해당 이메일의 Refresh Token 조회
        // key: "refresh:{이메일}"로 저장했으므로 동일한 key로 조회
        String savedRefreshToken = redisTemplate.opsForValue().get("refresh:" + email);

        // Redis에 없거나 (만료/로그아웃) 값이 다르면 (탈취 의심) 예외 처리
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new RuntimeException("Refresh Token이 일치하지 않거나 만료되었습니다.");
        }

        // 4. 검증 통과 → 새 Access Token 발급 (Refresh Token은 그대로 유지)
        log.info("Access Token 재발급 완료: {}", email);
        return jwtProvider.createToken(email);
    }
}