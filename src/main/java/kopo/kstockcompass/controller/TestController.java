package kopo.kstockcompass.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test")
    public String test() {
        return "K-Stock Compass 서버가 정상적으로 응답하고 있습니다! (4/5)";
    }
}