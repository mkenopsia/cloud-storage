package com.cloudstorage.controller.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPayload(
        @NotBlank(message = "users.errors.blank_username")
        @Size(min = 3, max = 15, message = "users.errors.invalid_username_size")
        String username,
        @NotBlank(message = "users.errors.blank_password")
        @Size(min = 5, max = 15, message = "users.errors.invalid_password_size")
        String password
) {}
