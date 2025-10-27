package com.minimart.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 🆕 Enable scheduled tasks (for cleanup)
@EnableAsync       // 🆕 Enable async tasks (for payment monitoring)
public class MinimartApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinimartApiApplication.class, args);
    }
}