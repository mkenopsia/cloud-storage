package com.cloudstorage.service.ResourceService;

import com.cloudstorage.controller.payload.DirectoryPayload;
import com.cloudstorage.controller.payload.FilePayload;
import io.minio.errors.*;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface FileService {
    List<FilePayload> uploadFile(String path, List<MultipartFile> files) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;

    FilePayload getFileInfo(String fullPath) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;

    void deleteFile(String path) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;

    InputStream downloadFile(String path, HttpServletResponse response) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;

    FilePayload renameFile(String oldName, String newName) throws NoSuchFileException, FileAlreadyExistsException;

    FilePayload moveFile(String from, String to) throws NoSuchFileException, FileAlreadyExistsException;

    List<FilePayload> findResources(String query);
}
