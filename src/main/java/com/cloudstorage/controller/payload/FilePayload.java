package com.cloudstorage.controller.payload;

import jakarta.validation.constraints.NotBlank;

public record FilePayload(
        String path,
        String name,
        Long size,
        String type
) {}
