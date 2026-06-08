package kopo.kstockcompass.service.impl;

import kopo.kstockcompass.service.IEmailVerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * [이메일 인증 서비스]
 * 설명:
 * 회원가입 시 이메일 인증번호를 생성하여 발송하고,
 * 사용자가 입력한 인증번호를 검증하는 서비스 계층입니다.
 *
 * 인증번호는 Redis에 일정 시간(TTL) 동안 저장하여
 * 빠른 조회와 자동 만료 처리를 구현했습니다.
 */
@Service
// 비즈니스 로직을 담당하는 서비스 계층임을 스프링에 알림
@RequiredArgsConstructor
// final 필드(redisTemplate, mailSender)를 자동으로 생성자 주입 (IoC/DI 원칙)
public class EmailVerifyService implements IEmailVerifyService {

    private final StringRedisTemplate redisTemplate; // Redis 접근 도구 (인증번호 저장/조회용)
    private final JavaMailSender mailSender; // 이메일 발송 도구 (SMTP 서버 연동)

    /**
     * [인증번호 발송]
     * 역할:
     * 6자리 인증번호를 생성하여 Redis에 저장하고 이메일로 발송합니다.
     *
     * 설명:
     * 인증번호는 Redis에 Key-Value 형태로 저장되며,
     * TTL(Time To Live)을 5분으로 설정하여
     * 일정 시간이 지나면 자동으로 삭제되도록 구성했습니다.
     *
     * Redis 저장 예시:
     * Key   -> verify:test@test.com
     * Value -> 123456
     */
    @Override
    public void sendCode(String email) {

        // 0 ~ 999999 범위의 난수를 생성 후 6자리 문자열로 변환
        String code = String.format("%06d",
                new Random().nextInt(1000000));

        // Redis 저장 (5분 유지 후 자동 만료)
        redisTemplate.opsForValue().set(
                "verify:" + email,
                code,
                5,
                TimeUnit.MINUTES
        );

        // 이메일 메시지 생성
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("[K-Stock Compass] 이메일 인증번호");

        message.setText(
                "인증번호: " + code +
                        "\n\n5분 이내에 입력해주세요."
        );

        // 메일 전송
        mailSender.send(message);
    }

    /**
     * [인증번호 검증]
     * 역할:
     * 사용자가 입력한 인증번호와 Redis에 저장된 인증번호를 비교합니다.
     *
     * 설명:
     * Redis에서 인증번호를 조회한 뒤,
     * 값이 존재하지 않으면 false를 반환합니다.
     *
     * 인증 성공 시:
     * 저장된 인증번호와 사용자가 입력한 값이 일치해야 합니다.
     */
    @Override
    public boolean verifyCode(String email, String code) {

        // Redis에 저장된 인증번호 조회
        String savedCode =
                redisTemplate.opsForValue().get("verify:" + email);

        // 인증번호가 없으면 실패
        // (TTL 만료 또는 잘못된 요청 상황)
        if (savedCode == null) {
            return false;
        }

        // 입력값과 저장값 비교
        return savedCode.equals(code);
    }
}