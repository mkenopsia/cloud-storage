package com.cloudstorage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.Validator;

@Configuration
public class SpringConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }
}
