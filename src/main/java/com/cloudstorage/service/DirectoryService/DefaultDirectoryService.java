package com.cloudstorage.service.DirectoryService;

import com.cloudstorage.controller.payload.DirectoryPayload;
import com.cloudstorage.controller.payload.FilePayload;
import com.cloudstorage.service.ResourceService.FileService;
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

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Override
    public DirectoryPayload getDirectoryInfo(String path) throws NoSuchFileException {
        if (!isDirectoryExists(path)) {
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
        if (!isDirectoryExists(path)) {
            throw new NoSuchFileException("minio.directory.error.directory_not_exists");
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
    public void downloadDirectory(String path, ServletOutputStream outputStream, HttpServletResponse response) throws NoSuchFileException {
        if (!isDirectoryExists(path)) {
            throw new NoSuchFileException("minio.directory.error.directory_not_exists");
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
    public DirectoryPayload renameDirectory(String oldDirectoryName, String newDirectoryName) throws NoSuchFileException {
        if (!isDirectoryExists(oldDirectoryName)) {
            throw new NoSuchFileException("minio.directory.error.directory_not_exists");
        }

        if (isDirectoryExists(newDirectoryName)) {
            throw new UnsupportedOperationException("minio.directory.error.already_exists");
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

                this.fileService.renameFile(oldFileName, newFileName);
            }
        } catch (Exception exception) {
            throw new RuntimeException();
        }

        return new DirectoryPayload(getDirectoryPath(newDirectoryName), getDirectoryName(newDirectoryName), "DIRECTORY");
    }

    @Override
    public DirectoryPayload moveDirectory(String fromDirectory, String toDirectory) throws NoSuchFileException {
        if (!isDirectoryExists(fromDirectory)) {
            throw new NoSuchFileException("minio.directory.error.directory_not_exists");
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

                this.fileService.moveFile(oldFileName, newDirectoryName);
            }
        } catch (Exception exception) {
            throw new RuntimeException();
        }

        return new DirectoryPayload(getDirectoryPathAfterMoving(toDirectory), getDirectoryName(fromDirectory), "DIRECTORY");
    }

    private String getDirectoryPathAfterMoving(String toDirectory) {
        if (toDirectory.equals("/")) return "/";

        return getDirectoryPath(toDirectory) + getDirectoryName(toDirectory) + "/";
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
        if (!isDirectoryExists(directoryPath)) {
            throw new NoSuchFileException("minio.directory.error.directory_not_exists");
        }

        List<FilePayload> resources = new ArrayList<>();

        try {
            for (var result : this.minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(directoryPath)
                            .build()
            )) {
                Item currItem = result.get();
                resources.add(new FilePayload(
                        getDirectoryPath(currItem.objectName()),
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
        if (!isDirectoryInRoot(directoryPath) && !isDirectoryExists(getDirectoryPath(directoryPath))) {
            throw new NoSuchFileException("minio.directory.error.parent_directory_not_exists");
        }

        if (isDirectoryExists(directoryPath)) {
            throw new UnsupportedOperationException("minio.directory.error.already_exists");
        }

        try {
            this.minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(directoryPath)
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

    private boolean isDirectoryInRoot(String directoryPath) {
        return directoryPath.split("/").length <= 1;
    }
}
