package com.cloudstorage.controller;

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
        if(path.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        if(files == null || files.isEmpty()) {
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
        if(path.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        if(path.charAt(path.length() - 1) == '/') {
            return ResponseEntity.ok(this.resourceService.getDirectoryInfo(path));
        }
        else {
            return ResponseEntity.ok(this.resourceService.getFileInfo(path));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteResource(@RequestParam("path") String path) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if(path.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        if(path.charAt(path.length() - 1) == '/') {
            this.resourceService.deleteDirectory(path);
        }
        else {
            this.resourceService.deleteFile(path);
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/download")
    public void downloadResource(@RequestParam("path") String path, HttpServletResponse response) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if(path.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        if(path.charAt(path.length() - 1) == '/') {
//            return ResponseEntity.ok(this.resourceService.downloadDirectory(path));
        }
        else {
            response.setStatus(HttpStatus.OK.value());
            response.setContentType("application/octet-stream");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path + "\"");
            StreamUtils.copy(this.resourceService.downloadFile(path), response.getOutputStream());
            response.flushBuffer();
        }
    }
}
