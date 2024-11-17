package com.spherelabs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CrossBorderApplication {
    public static void main(String[] args) {
        SpringApplication.run(CrossBorderApplication.class, args);
    }
}