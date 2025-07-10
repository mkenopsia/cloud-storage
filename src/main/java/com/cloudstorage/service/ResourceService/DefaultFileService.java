package com.cloudstorage.service.ResourceService;

import com.cloudstorage.controller.payload.FilePayload;
import com.cloudstorage.service.AuthService.AuthService;
import com.cloudstorage.utils.ResourcePathParseUtils;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DefaultFileService implements FileService {

    private final MinioClient minioClient;
    private final AuthService authService;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Override
    public List<FilePayload> uploadFile(String path, List<MultipartFile> files) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        List<FilePayload> uploadedFiles = new ArrayList<>();

        for (var file : files) {
            String fullFileName = getUserPrefix() + path + file.getOriginalFilename();

            if (isFileExists(fullFileName)) {
                throw new FileAlreadyExistsException(file.getOriginalFilename(), path, "minio.file.error.already_exists");
            }

            this.minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullFileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            uploadedFiles.add(createFilePayload(path, file.getOriginalFilename(), file.getSize(), "FILE"));
        }

        return uploadedFiles;
    }

    private String getUserPrefix() {
        return "user-%d-files/".formatted(this.authService.getUserIdFromSession());
    }

    private FilePayload createFilePayload(String path, String name, Long size, String type) {
        return new FilePayload(path, name, size, type);
    }

    private boolean isFileExists(String fileName) {
        try {
            this.minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public FilePayload getFileInfo(String fullFilePath) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String fullFileName = getUserPrefix() + fullFilePath;

        if (!isFileExists(fullFileName)) {
            throw new NoSuchFileException("minio.file.error.resource_not_found");
        }

        var fileInfo = this.minioClient.statObject(StatObjectArgs.builder()
                .bucket(bucketName)
                .object(fullFileName)
                .build());

        return createFilePayload(ResourcePathParseUtils.getFilePath(fullFilePath),
                ResourcePathParseUtils.getFileName(fullFilePath),
                fileInfo.size(),
                "FILE");
    }

    @Override
    public void deleteFile(String fullFilePath) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String fullFileName = getUserPrefix() + fullFilePath;

        if (!isFileExists(fullFileName)) {
            throw new NoSuchFileException("minio.file.error.resource_not_found");
        }

        this.minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(fullFileName)
                .build()
        );
    }

    @Override
    public InputStream downloadFile(String path, HttpServletResponse response) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String fullFileName = getUserPrefix() + path;

        if (!isFileExists(fullFileName)) {
            throw new NoSuchFileException("minio.file.error.resource_not_found");
        }

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + ResourcePathParseUtils.getFileName(fullFileName) + "\"");

        return this.minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullFileName)
                        .build()
        );
    }

    @Override
    public FilePayload renameFile(String oldName, String newName) throws NoSuchFileException, FileAlreadyExistsException {
        String fullOldFileName = getUserPrefix() + oldName;
        String fullNewFileName = getUserPrefix() + newName;

        if (!isFileExists(fullOldFileName)) {
            throw new NoSuchFileException("minio.file.error.resource_not_found");
        }

        if (isFileExists(fullNewFileName)) {
            throw new FileAlreadyExistsException(fullNewFileName,
                    ResourcePathParseUtils.getFilePath(fullNewFileName),
                    "minio.file.error.already_exists");
        }

        FilePayload filePayload;

        try {
            InputStream oldFile = this.minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullOldFileName)
                            .build()
            );

            StatObjectResponse oldFileInfo = this.minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullOldFileName)
                            .build()
            );

            this.deleteFile(fullOldFileName);

            this.minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullNewFileName)
                            .stream(oldFile, oldFileInfo.size(), -1)
                            .contentType(oldFileInfo.contentType())
                            .build()
            );

            filePayload = new FilePayload(ResourcePathParseUtils.getFilePath(newName),
                    ResourcePathParseUtils.getFileName(newName),
                    oldFileInfo.size(),
                    "FILE");
        } catch (Exception exception) {
            throw new RuntimeException();
        }

        return filePayload;
    }

    @Override
    public FilePayload moveFile(String file, String directoryForMoving) throws NoSuchFileException, FileAlreadyExistsException {
        String newFilePath = directoryForMoving + ResourcePathParseUtils.getFileName(file);

        return this.renameFile(file, newFilePath);
    }

    @Override
    public List<FilePayload> findResources(String query) {
        List<FilePayload> resources = new ArrayList<>();

        Map<String, Boolean> foundDirectories = new HashMap<>();

        try {
            Iterable<Result<Item>> results = this.minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(getUserPrefix())
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();

                if (!isDirectory(item.objectName()) && ResourcePathParseUtils.getFileName(item.objectName()).contains(query)) {
                    resources.add(this.createFilePayload(
                            ResourcePathParseUtils.getPathWithoutUserPrefix(item.objectName()),
                            ResourcePathParseUtils.getFileName(item.objectName()),
                            item.size(),
                            "FILE"
                    ));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException();
        }

        return resources;
    }

    private boolean isDirectory(String path) {
        return path.endsWith("/");
    }
}