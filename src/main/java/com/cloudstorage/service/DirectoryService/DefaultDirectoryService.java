package com.cloudstorage.service.DirectoryService;

import com.cloudstorage.controller.payload.DirectoryPayload;
import com.cloudstorage.controller.payload.FilePayload;
import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.service.AuthService.AuthService;
import com.cloudstorage.service.ResourceService.FileService;
import com.cloudstorage.service.UserService.UserService;
import com.cloudstorage.utils.ResourcePathParseUtils;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
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
public class DefaultDirectoryService implements DirectoryService {

    private final MinioClient minioClient;
    private final FileService fileService;
    private final AuthService authService;
    private final UserService userService;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Override
    public DirectoryPayload getDirectoryInfo(String path) throws NoSuchFileException {
        String fullDirectoryPath = getUserPrefix() + path;

        if (!isDirectoryExists(fullDirectoryPath)) {
            throw new NoSuchFileException("minio.directory.error.directory_not_exists");
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
        if (fullDirectoryPath.equals("/")) return "/";

        String[] parts = fullDirectoryPath.split("/");

        return parts[parts.length - 1];
    }

    private String getDirectoryPath(String fullDirectoryPath) {
        if (fullDirectoryPath.equals("/")) return "/";

        String[] parts = fullDirectoryPath.split("/");

        return Arrays.stream(parts)
                .limit(parts.length - 1)
                .collect(Collectors.joining("/")) + "/";
    }


    @Override
    public void deleteDirectory(String path) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String fullDirectoryPath = getUserPrefix() + path;

        if (!isDirectoryExists(fullDirectoryPath)) {
            throw new NoSuchFileException("minio.directory.error.directory_not_exists");
        }

        Iterable<Result<Item>> results = this.minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fullDirectoryPath)
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
    public void downloadDirectory(String path, ServletOutputStream outputStream, HttpServletResponse response) throws NoSuchFileException {
        String fullDirectoryPath = getUserPrefix() + path;

        if (!isDirectoryExists(fullDirectoryPath)) {
            throw new NoSuchFileException("minio.directory.error.directory_not_exists");
        }

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + getDirectoryName(fullDirectoryPath) + "\"");

        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            Iterable<Result<Item>> results = this.minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(fullDirectoryPath)
                            .recursive(true)
                            .build()
            );

            for (var result : results) {
                String zipEntryName = result.get().objectName().substring(fullDirectoryPath.length());

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
    public DirectoryPayload renameDirectory(String oldDirectoryName, String newDirectoryName) throws NoSuchFileException {
        String fullOldDirectoryPath = getUserPrefix() + oldDirectoryName;
        String fullNewDirectoryPath = getUserPrefix() + newDirectoryName;

        if (!isDirectoryExists(fullOldDirectoryPath)) {
            throw new NoSuchFileException("minio.directory.error.directory_not_exists");
        }

        if (isDirectoryExists(fullNewDirectoryPath)) {
            throw new UnsupportedOperationException("minio.directory.error.already_exists");
        }

        try {
            Iterable<Result<Item>> results = this.minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(fullOldDirectoryPath)
                            .recursive(true)
                            .build()
            );

            for (var result : results) {
                String oldFileName = result.get().objectName();
                String newFileName = fullNewDirectoryPath + oldFileName.substring(fullOldDirectoryPath.length());

                this.fileService.renameFile(oldFileName, newFileName);
            }
        } catch (Exception exception) {
            throw new RuntimeException();
        }

        return new DirectoryPayload(getDirectoryPath(newDirectoryName), getDirectoryName(newDirectoryName), "DIRECTORY");
    }

    @Override
    public DirectoryPayload moveDirectory(String fromDirectory, String toDirectory) throws NoSuchFileException {
        String fullFromDirectoryPath = getUserPrefix() + fromDirectory;
        String fullToDirectoryPath = getUserPrefix() + toDirectory;

        if (!isDirectoryExists(fullFromDirectoryPath)) {
            throw new NoSuchFileException("minio.directory.error.directory_not_exists");
        }

        try {
            Iterable<Result<Item>> results = this.minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(fullFromDirectoryPath)
                            .recursive(true)
                            .build()
            );

            for (var result : results) {
                String oldFileName = result.get().objectName();
                String newDirectoryName = fullToDirectoryPath + cutInnerDirectoryPath(oldFileName, fullFromDirectoryPath);

                this.fileService.moveFile(oldFileName, newDirectoryName);
            }
        } catch (Exception exception) {
            throw new RuntimeException();
        }

        return new DirectoryPayload(getDirectoryPathAfterMoving(toDirectory), getDirectoryName(toDirectory), "DIRECTORY");
    }

    private String getDirectoryPathAfterMoving(String toDirectory) {
        return ResourcePathParseUtils.getPathWithoutUserPrefix(toDirectory) + getDirectoryName(toDirectory) + "/";
    }

    /**
     * Метод нужен для таких случаев: пермещаем папку ...folder/ и у нее есть внутренняя папка
     * ...folder/innerFolder/file.txt
     * метод обрежет хвост (...) до папки folder и уберет название файла и выдаст в итоге внутреннюю структуру папок
     */
    private String cutInnerDirectoryPath(String fullPath, String fromDirectory) {
        return fullPath.substring(fromDirectory.length() - (getDirectoryName(fromDirectory)).length() - 1,
                fullPath.length() - ResourcePathParseUtils.getFileName(fullPath).length());
    }

    @Override
    public List<FilePayload> getDirectoryContent(String directoryPath) throws NoSuchFileException {
        String fullDirectoryPath = getUserPrefix() + directoryPath;

        if (!isDirectoryExists(fullDirectoryPath)) {
            throw new NoSuchFileException("minio.directory.error.directory_not_exists");
        }

        List<FilePayload> resources = new ArrayList<>();

        try {
            for (var result : this.minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(fullDirectoryPath)
                            .build()
            )) {
                Item currItem = result.get();
                resources.add(new FilePayload(
                        ResourcePathParseUtils.getPathWithoutUserPrefix(currItem.objectName()),
                        getDirectoryName(currItem.objectName()),
                        currItem.size(), (!currItem.objectName().endsWith("/")) ? "FILE" : "DIRECTORY")
                );
            }
        } catch (Exception exception) {
            throw new RuntimeException();
        }

        return resources;
    }

    @Override
    public DirectoryPayload createDirectory(String directoryPath) throws NoSuchFileException, UnsupportedOperationException {
        String fullDirectoryPath = getUserPrefix() + directoryPath;

        if (!isDirectoryInRoot(fullDirectoryPath) && !isDirectoryExists(getDirectoryPath(fullDirectoryPath))) {
            throw new NoSuchFileException("minio.directory.error.parent_directory_not_exists");
        }

        if (isDirectoryExists(fullDirectoryPath)) {
            throw new UnsupportedOperationException("minio.directory.error.already_exists");
        }

        try {
            this.minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullDirectoryPath)
                            .stream(InputStream.nullInputStream(), 0, -1)
                            .build()
            );
        } catch (Exception exception) {
            throw new RuntimeException();
        }

        return new DirectoryPayload(getDirectoryPath(directoryPath),
                getDirectoryName(directoryPath),
                "DIRECTORY");
    }

    @Override
    public void createRootDirectory(UserPayload userPayload) throws NoSuchFileException {
        Integer userId = this.userService.findByUsername(userPayload.username()).get().getId();
        String userRootDirectoryName = "user-%d-files/".formatted(userId);

        try {
            this.minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(userRootDirectoryName)
                            .stream(InputStream.nullInputStream(), 0, -1)
                            .build()
            );
        } catch (Exception exception) {
            throw new RuntimeException();
        }
    }

    private boolean isDirectoryInRoot(String directoryPath) {
        return directoryPath.split("/").length <= 1;
    }

    private String getUserPrefix() {
        return "user-%d-files/".formatted(this.authService.getUserIdFromSession());
    }
}
