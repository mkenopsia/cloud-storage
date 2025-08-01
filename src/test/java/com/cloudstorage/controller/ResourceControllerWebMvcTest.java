package com.cloudstorage.controller;

import com.cloudstorage.config.SecurityConfig;
import com.cloudstorage.controller.payload.DirectoryPayload;
import com.cloudstorage.controller.payload.FilePayload;
import com.cloudstorage.service.DirectoryService.DirectoryService;
import com.cloudstorage.service.ResourceService.FileService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ResourceController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = SecurityConfig.class))
class ResourceControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SecurityFilterChain securityFilterChain;

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private DirectoryService directoryService;

    @Test
    void testUploadFile_validRequest_returnsCreated() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.txt",
                "text/plain",
                "test dau".getBytes()
        );


        // When + then
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/resource")
                        .file(file)
                        .param("path", "folder1/folder2/"))
                .andExpect(status().isCreated());
    }

    @Test
    void testGetFileInfo_returnsCorrectInfo() throws Exception {
        // Given
        String filePath = "testFolder/test.txt";

        // When
        when(fileService.getFileInfo(filePath)).thenReturn(
                new FilePayload("testFolder/", "test.txt", 123L, "FILE"));


        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/resource")
                        .param("path", filePath))
                .andExpect(status().isOk());

        verify(fileService, times(1)).getFileInfo(eq(filePath));
    }

    @Test
    void testGetDirectoryInfo_returnsCorrectInfo() throws Exception {
        // Given
        String directoryPath = "testFolder/";

        // When
        when(directoryService.getDirectoryInfo(directoryPath)).thenReturn(
                new DirectoryPayload("testFolder/", "test.txt", "FILE"));


        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/resource")
                        .param("path", directoryPath))
                .andExpect(status().isOk());

        verify(directoryService, times(1)).getDirectoryInfo(eq(directoryPath));
    }

    @Test
    void testDeleteResource_successfulDeletion() throws Exception {
        // Given
        String filePath = "testFolder/test.txt";

        // When + Then
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/resource")
                        .param("path", filePath))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDownloadFile_successfulDownloading() throws Exception {
        // Given
        String filePath = "testFolder/test.txt";
        String fileContent = "inner text";

        // When
        when(fileService.downloadFile(eq(filePath), any(HttpServletResponse.class)))
                .thenReturn(new ByteArrayInputStream(fileContent.getBytes()));

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/resource/download")
                        .param("path", filePath))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(content().bytes(fileContent.getBytes()));

        verify(fileService, times(1)).downloadFile(eq(filePath), any(HttpServletResponse.class));
    }

    @Test
    void testDownloadDirectory_successfulDownloading() throws Exception {
        // Given
        String directoryPath = "testFolder/";

        // When + Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/resource/download")
                        .param("path", directoryPath))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"));

        verify(directoryService, times(1))
                .downloadDirectory(any(String.class), any(ServletOutputStream.class), any(HttpServletResponse.class));
    }

    @Test
    void testRenameFile_successfulRenaming() throws Exception {
        // Given
        String oldFilePath = "testFolder/test.txt";
        String newFilePath = "testFolder123/test.txt";

        // When
        when(fileService.renameFile(oldFilePath, newFilePath)).thenReturn(
                new FilePayload("testFolder123/", "test.txt", 123L, "FILE")
        );

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/resource/rename")
                        .param("oldName", oldFilePath)
                        .param("newName", newFilePath))
                .andExpect(status().isOk());

        verify(fileService, times(1)).renameFile(oldFilePath, newFilePath);
    }

    @Test
    void testRenameDirectory_successfulRenaming() throws Exception {
        // Given
        String oldDirectoryPath = "testFolder/";
        String newDirectoryPath = "testFolder123/";

        // When
        when(directoryService.renameDirectory(oldDirectoryPath, newDirectoryPath)).thenReturn(
                new DirectoryPayload("testFolder123/", "test.txt", "FILE")
        );

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/resource/rename")
                        .param("oldName", oldDirectoryPath)
                        .param("newName", newDirectoryPath))
                .andExpect(status().isOk());

        verify(directoryService, times(1)).renameDirectory(oldDirectoryPath, newDirectoryPath);
    }

    @Test
    void testMoveFile_successfulMoving() throws Exception {
        // Given
        String from = "testFolder/test.txt";
        String to = "testFolder123/";

        // When
        when(fileService.moveFile(from, to)).thenReturn(
                new FilePayload("testFolder123/", "test.txt", 123L, "FILE")
        );

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/resource/move")
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk());

        verify(fileService, times(1)).moveFile(from, to);
    }

    @Test
    void testMoveDirectory_successfulMoving() throws Exception {
        // Given
        String from = "testFolder/";
        String to = "testFolder123/";

        // When
        when(directoryService.moveDirectory(from, to)).thenReturn(
                new DirectoryPayload("testFolder123/", "test.txt", "FILE")
        );

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/resource/move")
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk());

        verify(directoryService, times(1)).moveDirectory(from, to);
    }

    @Test
    void testSearchResources_successfulSearching() throws Exception {
        // Given
        String query = "test";

        // When
        when(fileService.findResources(query)).thenReturn(
                List.of(new FilePayload("testFolder123/", "test.txt", 123L, "FILE"))
        );

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/resource/search")
                        .param("query", query))
                .andExpect(status().isOk());
 }
}