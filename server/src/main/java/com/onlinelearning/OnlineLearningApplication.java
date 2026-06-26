package com.onlinelearning;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.onlinelearning.mapper")
public class OnlineLearningApplication {
    public static void main(String[] args) {
        SpringApplication.run(OnlineLearningApplication.class, args);
    }
}
