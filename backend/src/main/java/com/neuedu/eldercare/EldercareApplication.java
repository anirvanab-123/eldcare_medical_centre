package com.neuedu.eldercare;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.neuedu.eldercare.mapper")
@SpringBootApplication
public class EldercareApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                EldercareApplication.class, args
        );
    }
}
