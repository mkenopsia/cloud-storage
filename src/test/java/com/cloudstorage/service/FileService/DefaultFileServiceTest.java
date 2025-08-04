package com.cloudstorage.service.FileService;

import com.cloudstorage.controller.payload.FilePayload;
import com.cloudstorage.service.AuthService.AuthService;
import com.cloudstorage.service.DirectoryService.DirectoryService;
import com.cloudstorage.service.UserService.UserService;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest
class DefaultFileServiceTest {

    private static final String ADMIN_ACCESS_KEY = "admin";
    private static final String ADMIN_SECRET_KEY = "12345678";
    private static final String TEST_BUCKET_NAME = "user-files";
    private static String minioEndpoint;

    private static GenericContainer<?> minioServer;

    @MockitoBean
    private UserService userService;

    @Autowired
    private FileService fileService;

    @MockitoBean
    private DirectoryService directoryService;

    @MockitoBean
    private AuthService authService;

    private static MinioClient client;

    @DynamicPropertySource
    public static void configureProperties(DynamicPropertyRegistry registry) throws Exception {
        registry.add("minio.accessKey", () -> ADMIN_ACCESS_KEY);
        registry.add("minio.secretKey", () -> ADMIN_SECRET_KEY);
        registry.add("minio.bucket.name", () -> TEST_BUCKET_NAME);

        int port = 9000;
        minioServer = new GenericContainer("minio/minio:latest")
                .withEnv("MINIO_ACCESS_KEY", ADMIN_ACCESS_KEY)
                .withEnv("MINIO_SECRET_KEY", ADMIN_SECRET_KEY)
                .withCommand("server /data")
                .withExposedPorts(port)
                .waitingFor(new HttpWaitStrategy()
                        .forPath("/minio/health/ready")
                        .forPort(port)
                        .withStartupTimeout(Duration.ofSeconds(3)));

        minioServer.start();

        Integer mappedPort = minioServer.getFirstMappedPort();
        org.testcontainers.Testcontainers.exposeHostPorts(mappedPort);
        minioEndpoint = String.format("http://%s:%s", minioServer.getHost(), mappedPort);

        minioServer.execInContainer(
                "sh", "-c",
                "mc config host add myminio http://localhost:9000 " + ADMIN_ACCESS_KEY + " " + ADMIN_SECRET_KEY + " --api s3v4");

        registry.add("minio.endpoint", () -> minioEndpoint);

        client = MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(ADMIN_ACCESS_KEY, ADMIN_SECRET_KEY)
                .build();
    }

//    @Test
//    public void canCreateBucketWithAdminUser() throws Exception {
//        client.ignoreCertCheck();
//        String bucketName = "testbucket";
//
//        client.makeBucket(MakeBucketArgs.builder()
//                .bucket(bucketName)
//                .build());
//
//        assertTrue(client.bucketExists(BucketExistsArgs.builder()
//                .bucket(bucketName)
//                .build()));
//    }

    @Test
    public void TestUploadFile_successfulUpload() throws Exception {
        // Given
        when(authService.getUserIdFromSession()).thenReturn(123);

        String filePath = "test/";
        String filename = "test.txt";
        String fullFilePath = "user-123-files/" + filePath + filename;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                "test dau".getBytes()
        );

        // When
        List<FilePayload> createdFiles = fileService.uploadFile(filePath, List.of(file));

        // Then
        assertNotNull(createdFiles);
        assertEquals(1, createdFiles.size());

        FilePayload payload = createdFiles.get(0);
        assertEquals(filename, payload.name());
        assertEquals(filePath, payload.path());
        assertEquals("FILE", payload.type());

        assertTrue(fileService.isFileExists(fullFilePath));
    }

    @Test
    public void TestGetFileInfo_successfulGet() throws Exception {
        // Given
        when(authService.getUserIdFromSession()).thenReturn(123);

        String filePath = "test/";
        String filename = "testGetInfo.txt";
        String fullFilePath = "user-123-files/" + filePath + filename;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                "test dau".getBytes()
        );

        fileService.uploadFile(filePath, List.of(file));

        // When + Then
        FilePayload result = fileService.getFileInfo(filePath + filename);

        assertEquals(filename, result.name());
        assertEquals(filePath, result.path());
        assertEquals("FILE", result.type());
    }

    @Test
    public void TestDeleteFile_successfulDeletion() throws Exception {
        // Given
        when(authService.getUserIdFromSession()).thenReturn(123);

        String filePath = "test/";
        String filename = "testDeletion.txt";
        String fullFilePath = "user-123-files/" + filePath + filename;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                "test dau".getBytes()
        );

        fileService.uploadFile(filePath, List.of(file));

        // When + Then
        fileService.deleteFile(filePath + filename);

        assertFalse(fileService.isFileExists(fullFilePath));
    }

    @Test
    public void TestDownloadFile_successfulDownloading() throws Exception {
        // Given
        when(authService.getUserIdFromSession()).thenReturn(123);

        String filePath = "test/";
        String filename = "testDownloading.txt";
        String fullFilePath = "user-123-files/" + filePath + filename;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                "test dau".getBytes()
        );

        fileService.uploadFile(filePath, List.of(file));

        // When + Then
        assertNotNull(fileService.downloadFile(filePath + filename, new MockHttpServletResponse()));
    }

    @Test
    public void TestRenameFile_successfulRenaming() throws Exception {
        // Given
        when(authService.getUserIdFromSession()).thenReturn(123);

        String filePath = "test/";
        String filename = "testRenaming.txt";
        String fullFilePath = "user-123-files/" + filePath + filename;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                "test dau".getBytes()
        );

        fileService.uploadFile(filePath, List.of(file));

        // When + Then
        String newName = "test/kek.txt";
        fileService.renameFile(filePath + filename, newName);

        assertFalse(fileService.isFileExists(fullFilePath));
        assertTrue(fileService.isFileExists("user-123-files/" + newName));
    }

    @Test
    public void TestMoveFile_successfulMoving() throws Exception {
        // Given
        when(authService.getUserIdFromSession()).thenReturn(123);

        String filePath = "test/";
        String filename = "testMoving.txt";
        String fullFilePath = "user-123-files/" + filePath + filename;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                "test dau".getBytes()
        );

        fileService.uploadFile(filePath, List.of(file));

        // When + Then
        String to = "test123/";
        fileService.moveFile(filePath + filename, to);

        assertFalse(fileService.isFileExists(fullFilePath));
        assertTrue(fileService.isFileExists("user-123-files/" + to + filename));
    }

    @Test
    public void TestSearching_successfulSearching() throws Exception {
        // Given
        when(authService.getUserIdFromSession()).thenReturn(123);

        String filePath = "test/";
        String filename = "testSearching.txt";
        String fullFilePath = "user-123-files/" + filePath + filename;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                "test dau".getBytes()
        );

        fileService.uploadFile(filePath, List.of(file));

        // When + Then
        List<FilePayload> foundFiles = fileService.findResources(filename);

        assertEquals(1, foundFiles.size());
    }

    @AfterAll
    static void shutDown() {
        if (minioServer.isRunning()) {
            minioServer.stop();
        }
    }
}