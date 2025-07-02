package com.cloudstorage.controller;

import com.cloudstorage.service.FileService.FileService;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resource")
public class FileController {

    private final FileService fileService;

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

        var response = this.fileService.uploadFile(path, files);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getFileInfo(@RequestParam("path") String path) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if(path.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        return ResponseEntity.ok(this.fileService.getFileInfo(path));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteFile(@RequestParam("path") String path) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if(path.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        this.fileService.deleteFile(path);
        return ResponseEntity.noContent().build();
    }
}
