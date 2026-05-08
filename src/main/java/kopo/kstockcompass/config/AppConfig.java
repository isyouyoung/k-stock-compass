package kopo.kstockcompass.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

// 이 클래스는 설정(Configuration) 파일
// 스프링이 켜질 때 "아, 이건 미리 준비해둬야겠다" 싶은 도구들을 여기서 정의해놓음
@Configuration
public class AppConfig {

    // WebClient라는 도구를 미리 만들어두는 거임.
    // 내 프로젝트가 외부(공공 API 서버 같은 곳)랑 통신을 해야 하니까
    // 매번 새로 만들기 귀찮아서 여기서 한 번 만들어놓고 필요할 때마다 빌려다 쓰려고 만들어놓음
    // 예전에는 RestTemplate을 많이 썼는데 교수님께서 이제 지원이 끝났다고 하시고
    // WebClient 이걸로 하였음
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    // API에서 데이터를 받아오면 그게 다 JSON(문자열) 형태임
    // 자바에서는 이걸 객체(DTO 같은 거)로 바꿔서 써야 함
    // 거꾸로 자바 객체를 JSON으로 바꿀 때도 사용함
    // 즉, JSON <-> 자바 객체 사이에서 번역기 역할을 해주는 도구임
    // Redis 캐싱할 때 데이터를 저장하거나 꺼내올 때도 사용함
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}