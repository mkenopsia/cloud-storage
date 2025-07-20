package com.cloudstorage.controller;

import com.cloudstorage.controller.payload.DirectoryPayload;
import com.cloudstorage.controller.payload.FilePayload;
import com.cloudstorage.service.DirectoryService.DirectoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.NoSuchFileException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/directory")
@Tag(name = "Контроллер директорий", description = "Обеспечивает логику работы с директориями")
public class DirectoryController {

    private final DirectoryService directoryService;

    @GetMapping
    @Operation(summary = "Получить содержимое директории", responses = {
            @ApiResponse(responseCode = "200",
                    description = "Успех",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = FilePayload.class))
                    )),
            @ApiResponse(responseCode = "400", description = "Невлидный или отсутствующий path", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "404", description = "Папка не существует", content = @Content),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка", content = @Content),
    })
    public ResponseEntity<List<FilePayload>> getDirectoryContent(
            @Parameter(name = "path", description = "Путь до директории, заканчивающийся на /", example = "folder1/folder2/", required = true)
            @RequestParam("path") String path
    ) throws NoSuchFileException {
        if (path.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        return ResponseEntity.ok(this.directoryService.getDirectoryContent(path));
    }

    @PostMapping
    @Operation(summary = "Создание пустой папки", responses = {
            @ApiResponse(responseCode = "201", description = "Успех", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DirectoryPayload.class))),
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий path", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "404", description = "Родительская папка не существует", content = @Content),
            @ApiResponse(responseCode = "409", description = "Папка с таким именем уще существует", content = @Content),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка", content = @Content)
    })
    public ResponseEntity<DirectoryPayload> createDirectory(
            @Parameter(name = "path", description = "Путь до директории, заканчивающийся на /", example = "folder1/folder2/", required = true)
            @RequestParam("path") String path
    ) throws NoSuchFileException {
        if (path.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(this.directoryService.createDirectory(path));
    }
}
