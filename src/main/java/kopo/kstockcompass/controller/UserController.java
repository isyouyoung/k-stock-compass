package kopo.kstockcompass.controller;

import kopo.kstockcompass.config.JwtProvider;
import kopo.kstockcompass.dto.*;
import kopo.kstockcompass.service.IEmailVerifyService;
import kopo.kstockcompass.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
     * 로직: 비밀번호는 BCrypt, 이메일/전화번호는 AES-128 CBC 암호화 후 DB 저장
     */
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody SignUpRequestDTO dto) {
        try {
            userService.signUp(dto);
            return ResponseEntity.ok("회원가입이 완료되었습니다.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * [2. 로그인 및 토큰 발급 API]
     * 과정: 이메일 AES-128 CBC 암호화 후 DB 조회 -> 비밀번호 BCrypt 검증 -> JWT 발급
     * 결과: Access Token + Refresh Token 반환
     * 특징: 세션을 서버에 저장하지 않는 Stateless 방식
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequestDTO dto) {
        try {
            Map<String, String> result = userService.login(dto);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * [3. 이메일 중복 확인 API]
     * 목적: 회원가입 폼에서 실시간으로 아이디(이메일) 사용 가능 여부를 피드백하기 위함.
     * 리턴: 중복이면 true, 사용 가능하면 false 반환.
     */
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String userEmail) {
        try {
            boolean isDuplicate = userService.checkEmail(userEmail);
            return ResponseEntity.ok(isDuplicate);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * [4. 이메일 인증번호 발송 API]
     * 비즈니스 로직: SMTP 서버 연동하여 6자리 인증 코드 전송
     * 보안: 인증 코드는 Redis에 5분 TTL로 저장되어 자동 만료
     * 보완: 발송 전 이미 가입된 회원인지 먼저 확인
     */
    @PostMapping("/send-code")
    public ResponseEntity<String> sendCode(@RequestBody EmailRequestDTO dto) {
        try {
            boolean isDuplicate = userService.checkEmail(dto.getUserEmail());
            if (isDuplicate) {
                return ResponseEntity.badRequest().body("이미 가입된 이메일입니다. 다른 이메일을 사용해주세요.");
            }
            emailVerifyService.sendCode(dto.getUserEmail());
            return ResponseEntity.ok("해당 이메일로 인증번호가 발송되었습니다. 3분 이내에 입력해주세요.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * [5. 인증번호 최종 검증 API]
     * 역할: 사용자가 메일로 받은 코드와 Redis에 저장된 코드를 비교하여 인증 처리
     * 중요성: 회원가입 전 실제 소유주임을 확인하는 핵심 보안 장치
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
     * 보안 처리: 이메일을 그대로 노출하지 않고 마스킹 처리하여 반환
     * 전화번호 AES-128 CBC 암호화 후 DB 조회, 이메일 복호화 후 마스킹
     */
    @GetMapping("/find-email")
    public ResponseEntity<String> findEmail(
            @RequestParam String userName,
            @RequestParam String userPnum) {
        try {
            String maskedEmail = userService.findEmail(userName, userPnum);
            return ResponseEntity.ok(maskedEmail);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * [7. 비밀번호 재설정 - 임시 비밀번호 발송]
     * 시나리오: 이름/이메일 확인 후 임시 비밀번호를 메일로 발송
     * 임시 비밀번호는 BCrypt 암호화 후 DB 저장
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestParam String userName,
            @RequestParam String userEmail) {
        try {
            userService.resetPassword(userName, userEmail);
            return ResponseEntity.ok("입력하신 이메일로 임시 비밀번호가 전송되었습니다. 로그인 후 반드시 변경해주세요.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * [8. 회원 전용: 비밀번호 변경 API]
     * @RequestHeader("Authorization"): HTTP 헤더에 담긴 JWT 토큰을 읽어옴
     * JWT에서 이메일 추출 후 AES-128 CBC 암호화하여 DB 조회
     * 현재 비밀번호 BCrypt 검증 후 새 비밀번호로 변경
     */
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestHeader("Authorization") String token,
            @RequestBody ChangePasswordRequestDTO dto) {
        try {
            String pureToken = token.replace("Bearer ", "");
            String email = jwtProvider.getEmail(pureToken);
            userService.changePassword(email, dto);
            return ResponseEntity.ok("비밀번호 변경이 정상적으로 완료되었습니다.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * [9. 회원 탈퇴 API]
     * JWT 토큰에서 이메일 추출 후 관련 데이터 전체 삭제
     * 삭제 순서: 알림로그 → 알림 → 관심종목 → 포트폴리오 → 계좌 → 시뮬레이터 → 자산히스토리 → 회원정보
     */
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUser(
            @RequestHeader("Authorization") String token) {
        try {
            String pureToken = token.replace("Bearer ", "");
            String email = jwtProvider.getEmail(pureToken);
            userService.deleteUser(email);
            return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * [Access Token 재발급 API]
     * 역할: Refresh Token으로 새 Access Token 발급
     * 흐름: Refresh Token 검증 → Redis 비교 → 새 Access Token 반환
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestHeader("Authorization") String token) {
        try {
            String refreshToken = token.replace("Bearer ", "").trim();
            String newAccessToken = userService.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류 발생"));
        }
    }

}