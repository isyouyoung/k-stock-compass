package kopo.kstockcompass.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정 클래스
 *
 * Spring Boot는 RedisTemplate을 자동으로 등록해주지만,
 * 기본 설정은 JdkSerializationRedisSerializer를 사용하기 때문에
 * 데이터가 바이너리 형태로 저장되어 Redis CLI에서 읽기 어렵고
 * 다른 언어/시스템과 호환이 안 될 수 있음.
 *
 * 따라서 이 설정 파일에서 RedisTemplate을 직접 Bean으로 등록하고
 * Key와 Value 모두 String(문자열) 형태로 직렬화되도록 명시적으로 설정함.
 */
@Configuration
// Spring 설정 클래스임을 선언
// 스프링이 시작될 때 이 파일을 읽어서 RedisTemplate Bean을 등록함
public class RedisConfig {

    /**
     * RedisTemplate Bean 등록
     *
     * RedisTemplate은 Redis 서버와 데이터를 주고받는 핵심 도구임.
     * 자바 객체를 Redis에 저장하거나 꺼낼 때 직렬화/역직렬화가 필요한데,
     * 여기서는 StringRedisSerializer를 사용해 모든 데이터를 String으로 처리함.
     *
     * @param connectionFactory Redis 서버와의 실제 연결을 담당하는 객체
     *                          (application.properties의 host, port, password 설정을 읽어 자동 생성됨)
     * @return 직렬화 설정이 완료된 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, String> template = new RedisTemplate<>();

        // Redis 서버와의 연결 설정 주입
        template.setConnectionFactory(connectionFactory);

        // Key 직렬화: Redis에 저장될 키를 String으로 변환
        // 예) "kis:access_token", "stock:price:005930" 같은 형태로 저장됨
        template.setKeySerializer(new StringRedisSerializer());

        // Value 직렬화: Redis에 저장될 값을 String으로 변환
        // 예) JWT 토큰 문자열, JSON 문자열 등을 그대로 저장
        template.setValueSerializer(new StringRedisSerializer());

        // Hash 자료구조 사용 시 Key 직렬화 설정
        template.setHashKeySerializer(new StringRedisSerializer());

        // Hash 자료구조 사용 시 Value 직렬화 설정
        template.setHashValueSerializer(new StringRedisSerializer());

        return template;
    }
}