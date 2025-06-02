package com.cloudstorage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("api.validation.user")
@Getter
@Setter
public class UserValidationProperties {
    private int minUsernameLength = 3;
    private int maxUsernameLength = 50;
    private int minPasswordLength = 3;
    private int maxPasswordLength = 50;
}
