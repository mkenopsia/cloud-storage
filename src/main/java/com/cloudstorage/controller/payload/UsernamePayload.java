package com.cloudstorage.controller.payload;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Имя созданного поьзователя")
public record UsernamePayload(
        @Schema(description = "Имя пользователя", example = "valid_username")
        String username
) {}
