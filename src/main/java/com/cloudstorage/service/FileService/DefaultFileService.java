package com.cloudstorage.service.FileService;

import com.cloudstorage.controller.payload.FilePayload;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
public class DefaultFileService implements FileService {

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
}
