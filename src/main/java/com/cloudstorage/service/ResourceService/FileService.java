package com.cloudstorage.service.ResourceService;

import com.cloudstorage.controller.payload.FilePayload;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.List;

public interface FileService {
    List<FilePayload> uploadFile(String path, List<MultipartFile> files) throws FileAlreadyExistsException;

    FilePayload getFileInfo(String fullPath) throws NoSuchFileException;

    void deleteFile(String path) throws NoSuchFileException;

    InputStream downloadFile(String path, HttpServletResponse response) throws NoSuchFileException;

    FilePayload renameFile(String oldName, String newName) throws NoSuchFileException, FileAlreadyExistsException;

    FilePayload moveFile(String from, String to) throws NoSuchFileException, FileAlreadyExistsException;

    List<FilePayload> findResources(String query);

    boolean isFileExists(String fileName);
}
