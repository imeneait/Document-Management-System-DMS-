package com.example.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.document.config.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class DocumentMicroserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocumentMicroserviceApplication.class, args);
    }
}
