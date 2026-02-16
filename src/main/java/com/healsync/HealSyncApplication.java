package com.healsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class HealSyncApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealSyncApplication.class, args);
    }
}
