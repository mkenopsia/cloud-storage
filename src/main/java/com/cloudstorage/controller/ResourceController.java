package com.cloudstorage.controller;

import com.cloudstorage.controller.payload.DirectoryPayload;
import com.cloudstorage.controller.payload.FilePayload;
import com.cloudstorage.service.ResourceService.ResourceService;
import io.minio.errors.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resource")
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    public ResponseEntity<?> uploadFile(@RequestParam("path") String path,
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

        var response = this.resourceService.uploadFile(path, files);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getResourceInfo(@RequestParam("path") String path) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (path.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        if (path.charAt(path.length() - 1) == '/') {
            return ResponseEntity.ok(this.resourceService.getDirectoryInfo(path));
        } else {
            return ResponseEntity.ok(this.resourceService.getFileInfo(path));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteResource(@RequestParam("path") String path) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (path.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        if (path.charAt(path.length() - 1) == '/') {
            this.resourceService.deleteDirectory(path);
        } else {
            this.resourceService.deleteFile(path);
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/download")
    public void downloadResource(@RequestParam("path") String path, HttpServletResponse response) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (path.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        if (path.endsWith("/")) {
            response.setContentType("application/zip");
            this.resourceService.downloadDirectory(path, response.getOutputStream(), response);
        } else {
            response.setContentType("application/octet-stream");
            StreamUtils.copy(this.resourceService.downloadFile(path, response), response.getOutputStream());
        }

        response.setStatus(HttpStatus.OK.value());
        response.flushBuffer();
    }

    @GetMapping("/rename")
    public ResponseEntity<?> renameResource(@RequestParam("oldName") String oldName,
                                            @RequestParam("newName") String newName) throws NoSuchFileException {
        if (oldName.isBlank() || newName.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        if (!oldName.endsWith("/") && !newName.endsWith("/")) {
            FilePayload filePayload = this.resourceService.renameFile(oldName, newName);
            return ResponseEntity.ok(filePayload);
        } else if (oldName.endsWith("/") && newName.endsWith("/")) {
            DirectoryPayload directoryPayload = this.resourceService.renameDirectory(oldName, newName);
            return ResponseEntity.ok(directoryPayload);
        } else {
            throw new UnsupportedOperationException("validation.error.logic.invalid_operation"); //TODO: сделать хендлер
        }
    }

    @GetMapping("/move")
    public ResponseEntity<?> moveResource(@RequestParam("from") String from,
                                          @RequestParam("to") String to) throws NoSuchFileException {
        if (from.isBlank() || to.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        if (from.endsWith("/") && to.endsWith("/")) {
            DirectoryPayload directoryPayload = this.resourceService.moveDirectory(from, to);
            return ResponseEntity.ok(directoryPayload);
        } else if(!from.endsWith("/") && to.endsWith("/")) {
            FilePayload filePayload = this.resourceService.moveFile(from, to);
            return ResponseEntity.ok(filePayload);
        }

        return ResponseEntity.ok().build();
    }
}
