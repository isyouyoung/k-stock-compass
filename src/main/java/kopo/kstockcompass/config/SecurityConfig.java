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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // // CSRF라는 보안 기능인데, 이걸 켜두면 Postman이나 프론트에서 API 쏠 때
                // 자꾸 가짜 아니야? 하고 막음. 그래서 개발 단계인 지금은 일단 꺼두었음
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // 모든 요청을 인증 없이 허용
                        // 지금은 .anyRequest().permitAll() 로 설정했음.
                        // 즉, "어떤 요청이든 일단 통과시켜라"라는 뜻임.
                        // 개발 초기에 API가 잘 작동하는지(Postman 테스트 등) 확인해야 하는데,
                        // 시큐리티가 사사건건 막으면 개발 진도가 너무 느리다고 해서 일단 대문을 활짝 열어둔 상태
                        // 나중에 개발이 다 끝나면 필요한 부분만 골라서 다시 막을 예정
                );

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