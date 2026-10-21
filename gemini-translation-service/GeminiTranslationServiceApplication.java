package com.example.geminitranslationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
public class GeminiTranslationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeminiTranslationServiceApplication.class, args);
    }
}