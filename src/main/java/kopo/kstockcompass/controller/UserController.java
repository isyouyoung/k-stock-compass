package kopo.kstockcompass.controller;

import kopo.kstockcompass.config.JwtProvider;
import kopo.kstockcompass.dto.*;
import kopo.kstockcompass.service.EmailVerifyService;
import kopo.kstockcompass.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final EmailVerifyService emailVerifyService;
    private final JwtProvider jwtProvider;

    // 회원가입 API
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody SignUpRequestDTO dto) {
        userService.signUp(dto);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    // 로그인 API
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequestDTO dto) {
        String result = userService.login(dto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String userEmail) {
        boolean isDuplicate = userService.checkEmail(userEmail);
        return ResponseEntity.ok(isDuplicate);
    }

    @PostMapping("/send-code")
    public ResponseEntity<String> sendCode(@RequestBody EmailRequestDTO dto) {
        emailVerifyService.sendCode(dto.getUserEmail());
        return ResponseEntity.ok("인증번호가 발송되었습니다.");
    }

    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@RequestBody VerifyCodeRequestDTO dto) {
        boolean result = emailVerifyService.verifyCode(dto.getUserEmail(), dto.getVerifyCode());
        if (result) {
            return ResponseEntity.ok("인증이 완료되었습니다.");
        } else {
            return ResponseEntity.badRequest().body("인증번호가 틀리거나 만료되었습니다.");
        }
    }

    // 아이디 찾기 API
    @GetMapping("/find-email")
    public ResponseEntity<String> findEmail(
            @RequestParam String userName,
            @RequestParam String userPnum) {
        String maskedEmail = userService.findEmail(userName, userPnum);
        return ResponseEntity.ok(maskedEmail);
    }

    // 비밀번호 변경 API (임시 비밀번호 발송)
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestParam String userName,
            @RequestParam String userEmail) {
        userService.resetPassword(userName, userEmail);
        return ResponseEntity.ok("임시 비밀번호가 이메일로 발송되었습니다.");
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestHeader("Authorization") String token,
            @RequestBody ChangePasswordRequestDTO dto) {
        String email = jwtProvider.getEmail(token.replace("Bearer ", ""));
        userService.changePassword(email, dto);
        return ResponseEntity.ok("비밀번호가 변경되었습니다.");
    }

}