package com.cloudstorage.service.ResourceService;

import com.cloudstorage.controller.payload.DirectoryPayload;
import com.cloudstorage.controller.payload.FilePayload;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import jakarta.servlet.ServletOutputStream;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        if (!isFileExists(fullFilePath)) {
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

        if(parts.length == 1) return "/";

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
        if (!isFileExists(fullFilePath)) {
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
        if(fullDirectoryPath.equals("/")) return "/";

        String[] parts = fullDirectoryPath.split("/");

        return parts[parts.length - 1];
    }

    private String getDirectoryPath(String fullDirectoryPath) {
        if(fullDirectoryPath.equals("/")) return "/";

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

        for (Result<Item> res : results) {
            this.minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(res.get().objectName())
                            .build()
            );
        }
    }

    @Override
    public InputStream downloadFile(String path, HttpServletResponse response) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (!isFileExists(path)) {
            throw new NoSuchFileException("minio.file.error.resource_not_found");
        }

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + getFileName(path) + "\"");

        return this.minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(path)
                        .build()
        );
    }

    @Override
    public void downloadDirectory(String path, ServletOutputStream outputStream, HttpServletResponse response) throws NoSuchFileException {
        if (!isDirectoryExists(path)) {
            throw new NoSuchFileException("minio.file.error.resource_not_found");
        }

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + getDirectoryName(path) + "\"");

        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            Iterable<Result<Item>> results = this.minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(path)
                            .recursive(true)
                            .build()
            );

            for (var result : results) {
                String zipEntryName = result.get().objectName().substring(path.length());

                zipOut.putNextEntry(new ZipEntry(zipEntryName));

                InputStream fileData = this.minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(result.get().objectName())
                                .build()
                );

                byte[] buffer = new byte[1024];
                int len;
                while ((len = fileData.read(buffer)) > 0) {
                    zipOut.write(buffer, 0, len);
                }

                zipOut.closeEntry();
            }
        } catch (Exception exception) {
            throw new RuntimeException(exception.getMessage());
        }
    }

    @Override
    public FilePayload renameFile(String oldName, String newName) throws NoSuchFileException {
        if (!isFileExists(oldName)) {
            throw new NoSuchFileException("minio.file.error.resource_not_found");
        }

        FilePayload filePayload;

        try {
            InputStream oldFile = this.minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(oldName)
                            .build()
            );

            StatObjectResponse oldFileInfo = this.minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(oldName)
                            .build()
            );

            this.deleteFile(oldName);

            this.minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(newName)
                            .stream(oldFile, oldFileInfo.size(), -1)
                            .contentType(oldFileInfo.contentType())
                            .build()
            );

            filePayload = new FilePayload(getFilePath(newName), getFileName(newName), oldFileInfo.size(), "FILE");
        } catch (Exception exception) {
            throw new RuntimeException();
        }

        return filePayload;
    }

    @Override
    public DirectoryPayload renameDirectory(String oldDirectoryName, String newDirectoryName) throws NoSuchFileException {
        if (!isDirectoryExists(oldDirectoryName)) {
            throw new NoSuchFileException("minio.file.error.resource_not_found");
        }

        try {
            Iterable<Result<Item>> results = this.minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(oldDirectoryName)
                            .recursive(true)
                            .build()
            );

            for (var result : results) {
                String oldFileName = result.get().objectName();
                String newFileName = newDirectoryName + oldFileName.substring(oldDirectoryName.length());

                this.renameFile(oldFileName, newFileName);
            }
        } catch (Exception exception) {
            throw new RuntimeException();
        }

        return new DirectoryPayload(getDirectoryPath(newDirectoryName), getDirectoryName(newDirectoryName), "DIRECTORY");
    }

    @Override
    public FilePayload moveFile(String from, String to) throws NoSuchFileException {
        if (!isFileExists(from)) {
            throw new NoSuchFileException("minio.file.error.resource_not_found");
        }

        FilePayload filePayload;

        try {
            InputStream fileData = this.minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(from)
                            .build()
            );

            StatObjectResponse fileInfo = this.minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(from)
                            .build()
            );

            this.deleteFile(from);

            String newFilePath = to + getFileName(from);

            this.minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(newFilePath)
                            .stream(fileData, fileInfo.size(), -1)
                            .contentType(fileInfo.contentType())
                            .build()
            );

            filePayload = new FilePayload(getFilePath(newFilePath), getFileName(from), fileInfo.size(), "FILE");
        } catch (Exception exception) {
            throw new RuntimeException();
        }

        return filePayload;
    }

    @Override
    public DirectoryPayload moveDirectory(String fromDirectory, String toDirectory) throws NoSuchFileException {
        if (!isDirectoryExists(fromDirectory)) {
            throw new NoSuchFileException("minio.file.error.resource_not_found");
        }

        try {
            Iterable<Result<Item>> results = this.minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(fromDirectory)
                            .recursive(true)
                            .build()
            );

            for (var result : results) {
                String oldFileName = result.get().objectName();
                String newDirectoryName = toDirectory + cutInnerDirectoryPath(oldFileName, fromDirectory);

                this.moveFile(oldFileName, newDirectoryName);
            }
        } catch (Exception exception) {
            throw new RuntimeException();
        }

        return new DirectoryPayload(getDirectoryPathAfterMoving(toDirectory), getDirectoryName(fromDirectory), "DIRECTORY");
    }

    private String getDirectoryPathAfterMoving(String toDirectory) {
        if(toDirectory.equals("/")) return "/";

        return getDirectoryPath(toDirectory) + getDirectoryName(toDirectory) + "/";
    }

    /**
     * Метод нужен для таких случаев: пермещаем папку ...folder/ и у нее есть внутренняя папка
     * ...folder/innerFolder/file.txt
     * метод обрежет хвост (...) до папки folder и уберет название файла и выдаст в итоге внутреннюю структуру папок
     */
    private String cutInnerDirectoryPath(String fullPath, String fromDirectory) {
        return fullPath.substring(fromDirectory.length() - (getDirectoryName(fromDirectory)).length() - 1,
                fullPath.length() - getFileName(fullPath).length());
    }
}

//test/
// folder/TestFolder

// test/test.py  ->  test/innerTest/testInner.py
// folder/innerFolder/
