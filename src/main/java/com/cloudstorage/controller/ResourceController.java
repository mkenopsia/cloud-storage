package com.cloudstorage.controller;

import com.cloudstorage.controller.payload.DirectoryPayload;
import com.cloudstorage.controller.payload.FilePayload;
import com.cloudstorage.service.DirectoryService.DirectoryService;
import com.cloudstorage.service.ResourceService.FileService;
import io.minio.errors.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resource")
@Tag(name = "Контроллер файлов", description = "Обеспечивает логику работы с ресурсами (файлами и папками)")
public class ResourceController {

    private final FileService fileService;
    private final DirectoryService directoryService;

    @PostMapping
    @Operation(summary = "Загрузить ресурс (файл или папку)", responses = {
            @ApiResponse(responseCode = "201", description = "Успех", content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = FilePayload.class))
            )),
            @ApiResponse(responseCode = "400", description = "Невалидное тело запроса", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "409", description = "Ресурс уже существует", content = @Content),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка", content = @Content),
    })
    public ResponseEntity<?> uploadFile(@Parameter(name = "path", description = "Путь до директории для загрузки, заканчивающийся на /", example = "folder1/folder2/", required = true)
                                        @RequestParam("path") String path,
                                        @Parameter(name = "files", description = "Файлы для загрузки из file input в формате MultipartFile", required = true)
                                        @RequestParam("files") List<MultipartFile> files) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (path.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("validation.error.files.no_files_present");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("validation.error.files.blank_file");
            }
        }

        var response = this.fileService.uploadFile(path, files);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Получить информацию о ресурсе (файле или папке)", responses = {
            @ApiResponse(responseCode = "200", description = "Успех", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = FilePayload.class)
            )),
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий путь", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден", content = @Content),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка", content = @Content),
    })
    public ResponseEntity<?> getResourceInfo(@Parameter(name = "path", description = "Путь до ресурса", example = "folder1/folder2/", required = true)
                                             @RequestParam("path") String path
    ) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (path.isBlank()) { //TODO убрать колбасы из ошибок
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        if (path.charAt(path.length() - 1) == '/') {
            return ResponseEntity.ok(this.directoryService.getDirectoryInfo(path));
        } else {
            return ResponseEntity.ok(this.fileService.getFileInfo(path));
        }
    }

    @DeleteMapping
    @Operation(summary = "Удалить ресурс (файл или папку)", responses = {
            @ApiResponse(responseCode = "204", description = "Успех", content = @Content),
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий путь", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден", content = @Content),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка", content = @Content),
    })
    public ResponseEntity<?> deleteResource(@Parameter(name = "path", description = "Путь до ресурса", example = "folder1/folder2/file.txt", required = true)
                                            @RequestParam("path") String path
    ) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (path.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        if (path.charAt(path.length() - 1) == '/') {
            this.directoryService.deleteDirectory(path);
        } else {
            this.fileService.deleteFile(path);
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/download")
    @Operation(summary = "Скачать ресурс (файл или папку (в формате zip))", responses = {
            @ApiResponse(responseCode = "200", description = "Успех", content = @Content(
                    mediaType = "application/octet-stream | application/zip"
            )),
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий путь", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден", content = @Content),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка", content = @Content),
    })
    public void downloadResource(@Parameter(name = "path", description = "Путь до ресурса", example = "folder1/folder2/file.txt", required = true)
                                 @RequestParam("path") String path, HttpServletResponse response
    ) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (path.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        if (path.endsWith("/")) {
            response.setContentType("application/zip");
            this.directoryService.downloadDirectory(path, response.getOutputStream(), response);
        } else {
            response.setContentType("application/octet-stream");
            StreamUtils.copy(this.fileService.downloadFile(path, response), response.getOutputStream());
        }

        response.setStatus(HttpStatus.OK.value());
        response.flushBuffer();
    }

    @GetMapping("/rename")
    @Operation(summary = "Переименовать ресурс (файл или папку)", responses = {
            @ApiResponse(responseCode = "200", description = "Успех", content = @Content),
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий путь", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден", content = @Content),
            @ApiResponse(responseCode = "409", description = "Ресурс по новому указанному пути уже существует", content = @Content),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка", content = @Content),
    })
    public ResponseEntity<?> renameResource(
            @Parameter(name = "oldName", description = "Путь до ресурса для переименования", example = "folder1/folder2/file.txt", required = true)
            @RequestParam("oldName") String oldName,
            @Parameter(name = "newName", description = "Новое имя ресурса", example = "folder1/folder2/newFile.txt", required = true)
            @RequestParam("newName") String newName
    ) throws NoSuchFileException, FileAlreadyExistsException {
        if (oldName.isBlank() || newName.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        if (!oldName.endsWith("/") && !newName.endsWith("/")) {
            FilePayload filePayload = this.fileService.renameFile(oldName, newName);
            return ResponseEntity.ok(filePayload);
        } else if (oldName.endsWith("/") && newName.endsWith("/")) {
            DirectoryPayload directoryPayload = this.directoryService.renameDirectory(oldName, newName);
            return ResponseEntity.ok(directoryPayload);
        } else {
            throw new UnsupportedOperationException("validation.error.logic.invalid_operation"); //TODO: сделать хендлер
        }
    }

    @GetMapping("/move")
    @Operation(summary = "Переместить ресурс (файл или папку)", responses = {
            @ApiResponse(responseCode = "200", description = "Успех", content = @Content),
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий путь", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден", content = @Content),
            @ApiResponse(responseCode = "409", description = "Ресурс по новому указанному пути уже существует", content = @Content),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка", content = @Content),
    })
    public ResponseEntity<?> moveResource(
            @Parameter(name = "from", description = "Путь до ресурса для перемещения", example = "folder1/folder2/file.txt", required = true)
            @RequestParam("from") String from,
            @Parameter(name = "to", description = "Новый путь ресурса", example = "folder1/file.txt", required = true)
            @RequestParam("to") String to
    ) throws NoSuchFileException, FileAlreadyExistsException {
        if (from.isBlank() || to.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        if (from.endsWith("/") && to.endsWith("/")) {
            DirectoryPayload directoryPayload = this.directoryService.moveDirectory(from, to);
            return ResponseEntity.ok(directoryPayload);
        } else if (!from.endsWith("/") && to.endsWith("/")) {
            FilePayload filePayload = this.fileService.moveFile(from, to);
            return ResponseEntity.ok(filePayload);
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Поиск по файлам", responses = {
            @ApiResponse(responseCode = "200", description = "Успех", content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = FilePayload.class))
            )),
            @ApiResponse(responseCode = "400", description = "Невалидное тело запроса", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка", content = @Content),
    })
    public ResponseEntity<?> searchResources(
            @Parameter(name = "query", description = "Поисковый запрос в URL-encoded формате", example = "file2", required = true)
            @RequestParam("query") String query) {
        if (query.isBlank()) {
            throw new IllegalArgumentException("validation.error.query.blank_query");
        }

        return ResponseEntity.ok(this.fileService.findResources(query));
    }
}
