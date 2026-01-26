package com.example.accesstoken.config;

import com.example.accesstoken.service.JwtKeyProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtKeyProperties.class)
public class JwtKeyConfiguration {

    @Bean
    public JwtKeyProvider jwtKeyProvider(JwtKeyProperties properties) {
        return new JwtKeyProvider(properties);
    }
}
