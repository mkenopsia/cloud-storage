package com.cloudstorage.service.DirectoryService;

import com.cloudstorage.controller.payload.DirectoryPayload;
import com.cloudstorage.controller.payload.FilePayload;
import com.cloudstorage.controller.payload.UserPayload;
import io.minio.errors.*;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface DirectoryService {

    DirectoryPayload getDirectoryInfo(String path) throws NoSuchFileException;

    void deleteDirectory(String path) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;

    void downloadDirectory(String path, ServletOutputStream outputStream, HttpServletResponse response) throws NoSuchFileException;

    DirectoryPayload renameDirectory(String oldName, String newName) throws NoSuchFileException;

    DirectoryPayload moveDirectory(String from, String to) throws NoSuchFileException;

    List<FilePayload> getDirectoryContent(String path) throws NoSuchFileException;

    DirectoryPayload createDirectory(String path) throws NoSuchFileException, UnsupportedOperationException;

    void createRootDirectory(UserPayload userPayload) throws NoSuchFileException;
}
