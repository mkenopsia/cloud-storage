package com.cloudstorage.service.ResourceService;

import com.cloudstorage.controller.payload.DirectoryPayload;
import com.cloudstorage.controller.payload.FilePayload;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultResourceService implements ResourceService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Override
    public List<FilePayload> uploadFile(String path, List<MultipartFile> files) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        List<FilePayload> uploadedFiles = new ArrayList<>();
        for (var file : files) {
            String fullFileName = path + file.getOriginalFilename();

            if (isResourceExists(fullFileName)) {
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

    private FilePayload createFilePayload(String path, String name, Long size, String type) {
        return new FilePayload(path, name, size, type);
    }

    private boolean isResourceExists(String fileName) {
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
        if (!isResourceExists(fullFilePath)) {
            throw new NoSuchFileException("minio.file.error.resource_not_found");
        }

        var fileInfo = this.minioClient.statObject(StatObjectArgs.builder()
                .bucket(bucketName)
                .object(fullFilePath)
                .build());

        return createFilePayload(getFilePath(fullFilePath), getFileName(fullFilePath), fileInfo.size(), "FILE");
    }

    private String getFilePath(String fullFilePath) {
        String[] parts = fullFilePath.split("/");

        return Arrays.stream(parts)
                .limit(parts.length - 1)
                .collect(Collectors.joining("/"));
    }

    private String getFileName(String fullFilePath) {
        String[] parts = fullFilePath.split("/");

        return parts[parts.length - 1];
    }

    @Override
    public void deleteFile(String fullFilePath) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (!isResourceExists(fullFilePath)) {
            throw new NoSuchFileException("minio.file.error.resource_not_found");
        }

        this.minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(fullFilePath)
                .build()
        );
    }

    @Override
    public DirectoryPayload getDirectoryInfo(String path) throws NoSuchFileException {
        if (!isDirectoryExists(path)) {
            throw new NoSuchFileException("minio.file.error.resource_not_found");
        }

        return new DirectoryPayload(getDirectoryPath(path), getDirectoryName(path), "DIRECTORY");
    }

    private boolean isDirectoryExists(String path) {
        Iterable<Result<Item>> results = this.minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(path)
                        .maxKeys(1)
                        .build()
        );

        return results.iterator().hasNext();
    }

    private String getDirectoryName(String fullDirectoryPath) {
        String[] parts = fullDirectoryPath.split("/");

        return parts[parts.length - 1];
    }

    private String getDirectoryPath(String fullDirectoryPath) {
        String[] parts = fullDirectoryPath.split("/");

        return Arrays.stream(parts)
                .limit(parts.length - 1)
                .collect(Collectors.joining("/")) + "/";
    }

    @Override
    public void deleteDirectory(String path) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (!isDirectoryExists(path)) {
            throw new NoSuchFileException("minio.file.error.resource_not_found");
        }

        Iterable<Result<Item>> results = this.minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(path)
                        .recursive(true)
                        .build()
        );

        for(Result<Item> res : results) {
            this.minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(res.get().objectName())
                    .build()
            );
        }
    }

    @Override
    public InputStream downloadFile(String fullFilePath) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (!isResourceExists(fullFilePath)) {
            throw new NoSuchFileException("minio.file.error.resource_not_found");
        }

        return this.minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullFilePath)
                        .build()
        );
    }
}
