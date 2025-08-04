package com.cloudstorage.service.DirectoryService;

import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.entity.User;
import com.cloudstorage.service.AuthService.AuthService;
import com.cloudstorage.service.FileService.FileService;
import com.cloudstorage.service.UserService.UserService;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest
class DefaultDirectoryServiceTest {
    private static final String ADMIN_ACCESS_KEY = "admin";
    private static final String ADMIN_SECRET_KEY = "12345678";
    private static final String TEST_BUCKET_NAME = "user-files";
    private static String minioEndpoint;

    private static GenericContainer<?> minioServer;

    @MockitoBean
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Autowired
    private DirectoryService directoryService;

    @MockitoBean
    private AuthService authService;

    private static MinioClient client;

    private boolean isRootDirectoryCreated = false;

    @BeforeEach
    void setUp() throws NoSuchFileException {
        if(!isRootDirectoryCreated) {
            when(userService.findByUsername(any(String.class)))
                    .thenReturn(Optional.of(new User(123, "test", "test")));

            directoryService.createUserRootDirectory(new UserPayload("test", "test"));
            isRootDirectoryCreated = true;
        }
    }

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

    @Test
    public void testCreateDirectory_successfulCreation() throws NoSuchFileException {
        // Given
        when(authService.getUserIdFromSession()).thenReturn(123);

        String directory = "test/";
        String fullDirectoryPath = "user-123-files/" + directory;

        directoryService.createDirectory(directory);

        // When + Then
        assertTrue(directoryService.isDirectoryExists(fullDirectoryPath));
    }

    @Test
    public void testGetDirectoryInfo_successfulGet() throws NoSuchFileException {
        // Given
        when(authService.getUserIdFromSession()).thenReturn(123);

        String directory = "testGetInfo/";
        String fullDirectoryPath = "user-123-files/" + directory;

        directoryService.createDirectory(directory);

        // When + Then
        assertNotNull(directoryService.getDirectoryInfo(directory));
    }

    @Test
    public void testDeleteDirectory_successfulDeletion() throws NoSuchFileException {
        // Given
        when(authService.getUserIdFromSession()).thenReturn(123);

        String directory = "testDeletion/";
        String fullDirectoryPath = "user-123-files/" + directory;

        directoryService.createDirectory(directory);

        // When + Then
        directoryService.deleteDirectory(directory);

        assertFalse(directoryService.isDirectoryExists(fullDirectoryPath));
    }

    @Test
    public void testRenameDirectory_successfulRenaming() throws NoSuchFileException {
        // Given
        when(authService.getUserIdFromSession()).thenReturn(123);

        String directory = "testRenaming/";
        String fullDirectoryPath = "user-123-files/" + directory;
        String newDirectoryName = "test123/";
        String newFullDirectoryPath = "user-123-files/" + newDirectoryName;

        directoryService.createDirectory(directory);

        // When + Then
        directoryService.renameDirectory(directory, newDirectoryName);

        assertFalse(directoryService.isDirectoryExists(fullDirectoryPath));
        assertTrue(directoryService.isDirectoryExists(newFullDirectoryPath));
    }

    @Test
    public void testRenameDirectoryWithSubfolder_successfulRenaming() throws NoSuchFileException {
        // Given
        when(authService.getUserIdFromSession()).thenReturn(123);

        String directory = "testRenaming2/";
        String innerDirectory = directory + "inner/";
        String fullDirectoryPath = "user-123-files/" + directory;
        String newDirectoryName = "renamedDirectory/";
        String newFullDirectoryPath = "user-123-files/" + newDirectoryName;
        String newFullInnerDirectoryPath = "user-123-files/" + newDirectoryName + "inner/";

        directoryService.createDirectory(directory);
        directoryService.createDirectory(innerDirectory);

        // When + Then
        directoryService.renameDirectory(directory, newDirectoryName);

        assertFalse(directoryService.isDirectoryExists(fullDirectoryPath));
        assertTrue(directoryService.isDirectoryExists(newFullDirectoryPath));
        assertTrue(directoryService.isDirectoryExists(newFullInnerDirectoryPath));
    }

    @Test
    public void testMoveDirectory_successfulMoving() throws NoSuchFileException {
        // Given
        when(authService.getUserIdFromSession()).thenReturn(123);

        String directory = "testMoving/";
        String fullDirectoryPath = "user-123-files/" + directory;
        String directoryForMoving = "moveHere/";
        String newFullDirectoryPath = "user-123-files/" + directoryForMoving;

        directoryService.createDirectory(directory);
        directoryService.createDirectory(directoryForMoving);

        // When + Then
        directoryService.moveDirectory(directory, directoryForMoving);

        assertFalse(directoryService.isDirectoryExists(fullDirectoryPath));
        assertTrue(directoryService.isDirectoryExists(newFullDirectoryPath + directory));
    }

    @Test
    public void testMoveDirectoryWithSubfolder_successfulMoving() throws NoSuchFileException {
        // Given
        when(authService.getUserIdFromSession()).thenReturn(123);

        String directory = "testMoving2/";
        String subfolder = "innerDirectory/";
        String fullDirectoryPath = "user-123-files/" + directory;
        String directoryForMoving = "moveHere2/";
        String newFullDirectoryPath = "user-123-files/" + directoryForMoving;

        directoryService.createDirectory(directory);
        directoryService.createDirectory(directory + subfolder);
        directoryService.createDirectory(directoryForMoving);

        // When + Then
        directoryService.moveDirectory(directory, directoryForMoving);

        assertFalse(directoryService.isDirectoryExists(fullDirectoryPath));
        assertTrue(directoryService.isDirectoryExists(newFullDirectoryPath + directory));
        assertTrue(directoryService.isDirectoryExists(newFullDirectoryPath + directory + subfolder));
    }

    @Test
    public void testGetDirectoryContent_successfulGet() throws NoSuchFileException, FileAlreadyExistsException {
        // Given
        when(authService.getUserIdFromSession()).thenReturn(123);

        String directory = "testGetContent/";
        String filename = "test.txt";
        String fullDirectoryPath = "user-123-files/" + directory;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                "test dau".getBytes()
        );

        directoryService.createDirectory(directory);
        fileService.uploadFile(directory, List.of(file));

        // When + Then
        assertNotNull(directoryService.getDirectoryContent(directory));
        assertEquals(1, directoryService.getDirectoryContent(directory).size());
    }
}