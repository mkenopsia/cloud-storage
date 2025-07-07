package com.cloudstorage.controller;

import com.cloudstorage.service.DirectoryService.DirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.NoSuchFileException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/directory")
public class DirectoryController {

    private final DirectoryService directoryService;

    @GetMapping
    public ResponseEntity<?> getDirectoryContent(@RequestParam("path") String path) throws NoSuchFileException {
        if (path.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        return ResponseEntity.ok(this.directoryService.getDirectoryContent(path));
    }

    @PostMapping
    public ResponseEntity<?> createDirectory(@RequestParam("path") String path) throws NoSuchFileException {
        if (path.isBlank()) {
            throw new IllegalArgumentException("validation.error.path.blank_path");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(this.directoryService.createDirectory(path));
    }
}
