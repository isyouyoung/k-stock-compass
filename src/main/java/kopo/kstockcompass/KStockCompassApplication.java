package kopo.kstockcompass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration; // 1. 이 줄 추가

// 2. exclude 설정을 추가해서 DB 연결 체크를 잠시 미룹니다. 추후 반드시 삭제할것!!!
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class KStockCompassApplication {

    public static void main(String[] args) {
        SpringApplication.run(KStockCompassApplication.class, args);
    }

}