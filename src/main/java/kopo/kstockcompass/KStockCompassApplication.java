package kopo.kstockcompass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class KStockCompassApplication {

    public static void main(String[] args) {
        SpringApplication.run(KStockCompassApplication.class, args);
    }

}