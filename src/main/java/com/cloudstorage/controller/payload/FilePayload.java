package com.cloudstorage.controller.payload;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Данные файла")
public record FilePayload(
        @Schema(name = "Путь до файла", example = "folder1/folder2/")
        String path,
        @Schema(name = "Имя файла", example = "test.txt")
        String name,
        @Schema(name = "Размер файлав байтах", example = "123")
        Long size,
        @Schema(name = "Тип данных", example = "FILE")
        String type
) {}
