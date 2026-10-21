package com.example.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {
    private String secret;
    private String tokenPrefix = "Bearer";
    private String headerName = "Authorization";
    private long expirationTime = 86400000;

    public String getSecret() {
    return secret;
}

public String getTokenPrefix() {
    return tokenPrefix;
}

public String getHeaderName() {
    return headerName;
}

public long getExpirationTime() {
    return expirationTime;
}
}

