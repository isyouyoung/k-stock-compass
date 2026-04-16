package kopo.kstockcompass.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EmailVerifyService {

    private final StringRedisTemplate redisTemplate;

    // 인증번호 생성 + Redis 저장 (TTL 5분)
    public String generateCode(String email) {

        // 6자리 랜덤 숫자 생성
        String code = String.format("%06d", new Random().nextInt(1000000));

        // Redis에 저장 (key: verify:이메일, value: 인증번호, TTL: 5분)
        redisTemplate.opsForValue().set(
                "verify:" + email,
                code,
                5,
                TimeUnit.MINUTES
        );

        return code;
    }

    // 인증번호 검증
    public boolean verifyCode(String email, String code) {

        String savedCode = redisTemplate.opsForValue().get("verify:" + email);

        if (savedCode == null) {
            return false; // 만료되거나 없음
        }

        return savedCode.equals(code);
    }
}