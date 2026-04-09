package kopo.kstockcompass.config;

// 역할: 애플리케이션의 출입 통제 시스템
//     설정: permitAll()을 사용해 모든 API를 일시적으로 개방했음
//     핵심 이유: 개발 초기 단계에서 스프링 시큐리티가 API 호출을 막아버리면 테스트(Postman 등)가 불가능하기 때문에,
//     **"일단 개발부터 편하게 하자"**는 전략으로 문을 열어두었음!
//     결과: 로그에 비밀번호가 떠도 무시하고 브라우저에서 404 페이지를 볼 수 있게 되었음

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // CSRF 보안 끄기 (Postman 테스트를 위해 필수)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // 모든 요청을 인증 없이 허용
                );

        return http.build();
    }
}