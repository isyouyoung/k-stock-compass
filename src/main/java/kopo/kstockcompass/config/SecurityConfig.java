package kopo.kstockcompass.config;

// 역할: 애플리케이션의 보안 설정 (인증/인가 정책)
// JWT + Stateless 구조에서는 실질적인 인증을 JwtFilter가 담당함
// 따라서 현재 개발 단계에서는 Security 단을 permitAll()로 열어두고 사용함
// 토큰 없거나 만료된 요청은 JwtFilter에서 401 반환,
// SecurityContext가 비어있으면 컨트롤러에서 인증 실패 처리됨

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
// Spring 설정 클래스임을 선언 - 스프링이 시작될 때 이 파일을 읽어서 보안 규칙을 적용함
@EnableWebSecurity
// Spring Security를 활성화 - 이게 없으면 보안 설정이 아예 적용 안 됨
@RequiredArgsConstructor
// final 필드(JwtFilter)를 자동으로 생성자 주입해주는 Lombok 어노테이션
public class SecurityConfig {

    // JwtFilter를 new로 생성하지 않고 DI로 주입받음 (Spring 정석 IoC/DI 구조)
    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // JWT + Stateless 구조에서는 CSRF를 비활성화하는 것이 정석
                // CSRF 공격은 세션 쿠키를 자동 전송하는 방식을 악용하는데,
                // 내 프로젝트는 세션 대신 Authorization 헤더에 JWT를 직접 담아 전송하므로
                // 브라우저가 자동으로 인증 정보를 보내는 일이 없어 CSRF 위협이 존재하지 않음

                .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        // 세션도 쓰고 JWT도 쓰면 의미가 없는 이유
                        // 세션 방식은 서버 메모리에 로그인 상태를 저장하는 방식(Stateful)이고, JWT는 토큰 자체에 회원 정보를 담아
                        // 서버를 가볍게 유지하는 방식(Stateless)인대
                        // 두 개를 동시에 켜두면 JWT의 장점인 '서버의 무상태성(가벼움)'을 전혀 살리지 못하고 리소스만 낭비됨
                        // 따라서 Spring Session 사용 안 하고 JWT만 사용하게 함
                )
                .authorizeHttpRequests(auth -> auth
                                .anyRequest().permitAll()
                        // JWT 방식에서는 permitAll()이어도 보안에 문제없음
                        // 실질적인 인증은 앞단의 JwtFilter에서 담당하기 때문
                        // 토큰이 없거나 만료된 요청은 JwtFilter에서 401을 반환하거나
                        // SecurityContext가 비어있어 컨트롤러에서 인증 실패 처리됨
                )
                // [JwtFilter 등록 - DI 방식]
                // new JwtFilter() 대신 Spring이 관리하는 Bean을 주입받아 사용
                // IoC/DI 원칙을 지킨 Spring Security 정석 구조
                // 모든 요청에서 JWT 토큰을 검증하고 SecurityContext에 인증 정보를 저장함
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
                // UsernamePasswordAuthenticationFilter보다 앞에 JwtFilter를 실행
                // 즉, 스프링 시큐리티의 기본 로그인 필터보다 먼저 JWT 검증을 수행함

        return http.build();
    }

    // PasswordEncoder Bean 등록
    // [비밀번호 암호화 도구]
    // 사용자가 회원가입할 때 입력한 비밀번호(예: 1234)를
    // DB에 그대로 저장하면 나중에 해킹당했을 때 큰일 남.
    // 그래서 BCrypt라는 강력한 알고리즘을 써서 아무도 못 알아보게 암호화해주는 도구를 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}