package com.cloudstorage.controller.payload;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Данные папки")
public record DirectoryPayload(
        @Schema(description = "Путь до папки", example = "folder1/folder2/")
        String path,
        @Schema(description = "Название папки", example = "folder3")
        String name,
        @Schema(description = "Тип данных", example = "DIRECTORY")
        String type
) {}
