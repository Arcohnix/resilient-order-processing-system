package com.sanket.resilientordersystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ResilientOrderSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                ResilientOrderSystemApplication.class,
                args
        );
    }
}