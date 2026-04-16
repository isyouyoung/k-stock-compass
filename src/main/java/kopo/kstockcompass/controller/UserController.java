package kopo.kstockcompass.controller;

import kopo.kstockcompass.dto.LoginRequestDTO;
import kopo.kstockcompass.dto.SignUpRequestDTO;
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

    // 이메일 중복 체크 API
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
        boolean isDuplicate = userService.checkEmail(email);
        return ResponseEntity.ok(isDuplicate);
    }

    // 이메일 인증번호 발송 API (임시 - 콘솔 출력)
    @PostMapping("/send-code")
    public ResponseEntity<String> sendCode(@RequestParam String email) {
        String code = emailVerifyService.generateCode(email);
        System.out.println("인증번호 [" + email + "] : " + code); // 임시 콘솔 출력
        return ResponseEntity.ok("인증번호가 발송되었습니다.");
    }

    // 인증번호 확인 API
    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(
            @RequestParam String email,
            @RequestParam String code) {
        boolean result = emailVerifyService.verifyCode(email, code);
        if (result) {
            return ResponseEntity.ok("인증이 완료되었습니다.");
        } else {
            return ResponseEntity.badRequest().body("인증번호가 틀리거나 만료되었습니다.");
        }
    }

}