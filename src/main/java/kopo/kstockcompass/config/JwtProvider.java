package kopo.kstockcompass.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

// @Component는 스프링한테 "이 클래스는 내가 인증 도구로 쓸 거니까 네가 관리해줘"라고 말하는 것
// 이 파일은 우리 서비스만의 '전자 도장'을 찍고, 그 도장이 진짜인지 검사하는 역할
@Component
public class JwtProvider {

    private final Key key;           // 우리만 아는 비밀 암호 키 (도장 디자인 같은 거)
    private final long expiration;   // 이 토큰이 언제까지 유효한지 유통기한
    private final long refreshExpiration;

    // application.properties 파일에 미리 적어둔 비밀 키랑 유통기한을 가져와서 세팅하는 부분
    // 도장을 찍으려면 인감 도장이랑 인코 패드가 있어야 하니까 그걸 준비하는 과정
    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        // 비밀 키를 컴퓨터가 이해할 수 있는 복잡한 해시 키 형태로 변환
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
    }

    // [토큰 생성] 로그인이 성공하면 이 메서드를 불러서 사용자의 '전자 출입증'을 발급
    public String createToken(String userEmail) {
        return Jwts.builder()
                .setSubject(userEmail) // 이 출입증의 주인 이름(이메일)을 적음
                .setIssuedAt(new Date()) // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // 언제까지 쓸 수 있는지 유통기한
                .signWith(key, SignatureAlgorithm.HS256) // 우리만 아는 키로 위조 못하게 암호화 사인을 함
                .compact(); // 이 모든 내용을 한 줄의 긴 글자(토큰)로 압축함
    }

    // [이메일 추출] 출입증을 들고 오면 이게 누구건지 확인함
    public String getEmail(String token) {
        return getClaims(token).getSubject();
    }

    // [유효성 검사] 이 출입증이 가짜는 아닌지, 유통기한이 지나진 않았는지 체크
    public boolean validateToken(String token) {
        try {
            getClaims(token); // 토큰 뚜껑을 열어보는 시도
            return true; // 아무 문제 없으면 통과
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw e; // 만료 예외는 다시 던져서 JwtFilter가 잡게!
        } catch (Exception e) {
            return false; // 뚜껑 열다가 에러 나면(가짜거나 만료거나) 막아버림
        }
    }

    // [Claims 추출] 토큰이라는 봉투를 뜯어서 그 안에 적힌 정보들(이름, 날짜 등)을 꺼내는 내부 로직임.
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key) // 우리가 찍었던 도장이랑 맞는지 확인
                .build()
                .parseClaimsJws(token) // 봉투를 뜯어서
                .getBody(); // 내용물을 꺼냄
    }

    // [Refresh Token 생성]
    // Access Token이 만료되었을 때 재발급을 위해 사용하는 장기 토큰 생성
    // Access Token(10분)보다 만료 시간이 길며(예: 7일),
    // 생성 후에는 Redis에 저장하여 유효성을 관리한다.
    public String createRefreshToken(String userEmail) {
        return Jwts.builder()
                .setSubject(userEmail)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}