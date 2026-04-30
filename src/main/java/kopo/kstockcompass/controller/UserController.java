package kopo.kstockcompass.controller;

import kopo.kstockcompass.config.JwtProvider;
import kopo.kstockcompass.dto.*;
import kopo.kstockcompass.service.IEmailVerifyService;
import kopo.kstockcompass.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 유저 컨트롤러는 사용자 인증과 인가의 모든 입구를 관리
// 데이터는 엔티티말고 DTO를 통함
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final IUserService userService;
    private final IEmailVerifyService emailVerifyService;
    private final JwtProvider jwtProvider;

    /**
     * [1. 회원가입 API]
     * @Valid: DTO에 설정한 검증 로직(@Email, @Size 등)을 활성화
     * @RequestBody: 클라이언트가 보낸 JSON 데이터를 SignUpRequestDTO 객체로 자동 변환(매핑)
     * 로직: 입력된 비밀번호는 서비스 레이어에서 BCrypt 알고리즘으로 암호화되어 DB에 저장
     */
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody SignUpRequestDTO dto) {
        userService.signUp(dto);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    /**
     * [2. 로그인 및 토큰 발급 API]
     * 과정: 사용자가 아이디/비번 제출 -> DB 대조 -> 일치 시 서버의 비밀키로 서명된 JWT 발급.
     * 결과: 결과값(String)으로 JWT 토큰이 반환되며, 프론트엔드는 이를 localStorage 등에 저장해 사용함.
     * 특징: 세션을 서버에 저장하지 않는 'Stateless' 방식을 구현하여 서버 자원 효율을 극대화함.
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequestDTO dto) {
        // userService.login은 성공 시 JWT 문자열을 리턴하도록 설계됨
        String result = userService.login(dto);
        return ResponseEntity.ok(result);
    }

    /**
     * [3. 이메일 중복 확인 API]
     * 목적: 회원가입 폼에서 실시간으로 아이디(이메일) 사용 가능 여부를 피드백하기 위함.
     * 리턴: 중복이면 true, 사용 가능하면 false 반환.
     */
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String userEmail) {
        boolean isDuplicate = userService.checkEmail(userEmail);
        return ResponseEntity.ok(isDuplicate);
    }

    /**
     * [4. 이메일 인증번호 발송 API]
     * 비즈니스 로직: 실제 SMTP 서버를 연동하여 사용자의 메일함으로 6자리 인증 코드를 전송함.
     * 보안: 이 단계에서 생성된 코드는 서버 메모리(또는 Redis)에 일정 시간(예: 5분) 동안만 유효하게 저장됨
     * 보완: 메일을 보내기 전에 이미 가입된 회원인지 먼저 확인합니다.
     */
    @PostMapping("/send-code")
    public ResponseEntity<String> sendCode(@RequestBody EmailRequestDTO dto) {
        // 1. 중복 체크 먼저 수행
        boolean isDuplicate = userService.checkEmail(dto.getUserEmail());

        if (isDuplicate) {
            // 이미 가입된 이메일이면 메일을 보내지 않고 에러 메시지 반환
            return ResponseEntity.badRequest().body("이미 가입된 이메일입니다. 다른 이메일을 사용해주세요.");
        }

        // 2. 중복이 아닐 때만 인증번호 발송
        emailVerifyService.sendCode(dto.getUserEmail());
        return ResponseEntity.ok("해당 이메일로 인증번호가 발송되었습니다. 3분 이내에 입력해주세요.");
    }

    /**
     * [5. 인증번호 최종 검증 API]
     * 역할: 사용자가 메일로 받은 코드와 화면에 입력한 코드가 일치하는지 최종 확인.
     * 중요성: 회원가입 전 '실제 소유주'임을 확인하여 허위 계정 생성을 방지하는 핵심 보안 장치임.
     */
    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@RequestBody VerifyCodeRequestDTO dto) {
        boolean result = emailVerifyService.verifyCode(dto.getUserEmail(), dto.getVerifyCode());
        if (result) {
            return ResponseEntity.ok("이메일 소유권 인증에 성공하였습니다.");
        } else {
            return ResponseEntity.badRequest().body("인증번호가 일치하지 않거나 유효 시간이 만료되었습니다.");
        }
    }

    /**
     * [6. 아이디(이메일) 찾기 API]
     * 보안 처리: 이메일을 그대로 노출하지 않고 'yo***@gmail.com'과 같이 마스킹 처리하여 반환.
     */
    @GetMapping("/find-email")
    public ResponseEntity<String> findEmail(
            @RequestParam String userName,
            @RequestParam String userPnum) {
        String maskedEmail = userService.findEmail(userName, userPnum);
        return ResponseEntity.ok(maskedEmail);
    }

    /**
     * [7. 비밀번호 재설정 - 임시 비밀번호 발송]
     * 시나리오: 비번 분실 시 이름/이메일 확인 후 랜덤하게 생성된 임시 비밀번호를 메일로 쏴줌.
     * 주의: 임시 비밀번호 역시 DB에는 암호화되어 저장되므로 보안상 안전함.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestParam String userName,
            @RequestParam String userEmail) {
        userService.resetPassword(userName, userEmail);
        return ResponseEntity.ok("입력하신 이메일로 임시 비밀번호가 전송되었습니다. 로그인 후 반드시 변경해주세요.");
    }

    /**
     * [8. 회원 전용: 비밀번호 변경 API]
     * @RequestHeader("Authorization"): HTTP 헤더에 담긴 JWT 토큰을 읽어옴.
     * 검증 과정:
     * 1. 헤더의 "Bearer " 접두사 제거 후 순수 토큰 추출.
     * 2. jwtProvider를 통해 토큰 내부에 숨겨진 사용자 이메일(Subject)을 꺼냄.
     * 3. 별도의 ID 입력 없이도 '현재 로그인한 본인'의 비밀번호만 안전하게 변경 가능함.
     * 핵심 기술: 이 API는 인가(Authorization)가 완료된 사용자만 접근 가능한 '보안 구역'임.
     */
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestHeader("Authorization") String token,
            @RequestBody ChangePasswordRequestDTO dto) {

        // 토큰 위변조 검사 및 사용자 식별 JWT
        String pureToken = token.replace("Bearer ", "");
        // 헤더에서 토큰을 꺼내서 이메일을 추출
        String email = jwtProvider.getEmail(pureToken);

        userService.changePassword(email, dto);
        return ResponseEntity.ok("비밀번호 변경이 정상적으로 완료되었습니다.");
    }

}