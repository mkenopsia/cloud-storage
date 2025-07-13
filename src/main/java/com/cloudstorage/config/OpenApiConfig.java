package com.cloudstorage.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "Cloud storage API",
                description = "API для взаимодействия с облачным хранилищем",
                version = "1.0.0",
                contact = @Contact(
                        name = "mkenopsia",
                        email = "gxkurro@yandex.ru"
                )
        )
)
public class OpenApiConfig {
}
