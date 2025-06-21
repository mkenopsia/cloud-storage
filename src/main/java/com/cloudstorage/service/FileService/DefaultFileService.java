package com.cloudstorage.service.FileService;

import com.cloudstorage.controller.payload.FilePayload;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultFileService implements FileService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;

    public List<FilePayload> uploadFile(String path, List<MultipartFile> files) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        List<FilePayload> uploadFiles = new ArrayList<>();
        for(var file : files) {
            String fullFileName = path + file.getOriginalFilename();

            if (isObjectExists(fullFileName)) {
                throw new FileAlreadyExistsException(file.getOriginalFilename(), path, "minio.file.error.already_exists");
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullFileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            uploadFiles.add(createFilePayload(path, file.getOriginalFilename(), file.getSize(), "FILE"));
        }

        return uploadFiles;
    }

    private FilePayload createFilePayload(String path, String name, Long size, String type) {
        return new FilePayload(path, name, size, type);
    }

    private boolean isObjectExists(String fileName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
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

}
