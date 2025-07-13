package com.cloudstorage.controller.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Данные пользователя")
public record UserPayload(
        @Schema(description = "Имя пользователя", example = "valid_username")
        @NotBlank(message = "users.errors.blank_username")
        @Size(min = 3, max = 15, message = "users.errors.invalid_username_size")
        String username,
        @Schema(description = "Имя пользователя", example = "valid_password")
        @NotBlank(message = "users.errors.blank_password")
        @Size(min = 5, max = 15, message = "users.errors.invalid_password_size")
        String password
) {}
