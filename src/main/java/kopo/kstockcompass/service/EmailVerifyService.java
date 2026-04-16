package kopo.kstockcompass.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EmailVerifyService {

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;

    // 인증번호 생성 + Redis 저장 + 이메일 발송
    public void sendCode(String email) {

        // 6자리 랜덤 숫자 생성
        String code = String.format("%06d", new Random().nextInt(1000000));

        // Redis에 저장 (TTL 5분)
        redisTemplate.opsForValue().set(
                "verify:" + email,
                code,
                5,
                TimeUnit.MINUTES
        );

        // 이메일 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[K-Stock Compass] 이메일 인증번호");
        message.setText("인증번호: " + code + "\n\n5분 이내에 입력해주세요.");
        mailSender.send(message);
    }

    // 인증번호 검증
    public boolean verifyCode(String email, String code) {

        String savedCode = redisTemplate.opsForValue().get("verify:" + email);

        if (savedCode == null) {
            return false;
        }

        return savedCode.equals(code);
    }
}