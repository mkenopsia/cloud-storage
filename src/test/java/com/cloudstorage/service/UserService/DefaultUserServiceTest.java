package com.cloudstorage.service.UserService;

import com.cloudstorage.config.MinioBucketInitializer;
import com.cloudstorage.config.MinioConfig;
import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.entity.User;
import com.cloudstorage.service.DirectoryService.DirectoryService;
import com.cloudstorage.service.ResourceService.FileService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
@SpringBootTest
class DefaultUserServiceTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private UserService userService;

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private DirectoryService directoryService;

    @MockitoBean
    private MinioConfig minioConfig;

    @MockitoBean
    private MinioBucketInitializer minioBucketInitializer;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @DynamicPropertySource
    public static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void testSaveUser_successfulSavingToDatabase() {
        // Given
        UserPayload userPayload = new UserPayload("test123", "123321");

        // When
        userService.save(userPayload);

        // Then
        User registeredUser = userService.findByUsername("test123").get();
        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getId()).isPositive();
        assertThat(encoder.matches("123321", registeredUser.getPassword())).isTrue();
    }

    @Test
    void testSaveUser_userAlreadyExists() {
        // Given
        UserPayload userPayload = new UserPayload("test1234", "123321");
        userService.save(userPayload);

        // When + Then
        assertThrows(IllegalStateException.class, () -> userService.save(userPayload));
    }

    @Test
    void testDeleteUser_successfulDeletion() {
        // Given
        UserPayload userPayload = new UserPayload("testUser", "123321");
        userService.save(userPayload);
        Integer userId = userService.findByUsername(userPayload.username()).get().getId();

        // When
        userService.deleteById(userId);

        // Then
        assertThat(userService.isUserExists(userPayload)).isFalse();
    }

    @AfterAll
    static void shutDown() {
        if (postgres.isRunning()) {
            postgres.stop();
        }
    }
}