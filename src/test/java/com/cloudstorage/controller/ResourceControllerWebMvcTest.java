package com.cloudstorage.controller;

import com.cloudstorage.service.DirectoryService.DirectoryService;
import com.cloudstorage.service.ResourceService.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ResourceController.class)
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
}