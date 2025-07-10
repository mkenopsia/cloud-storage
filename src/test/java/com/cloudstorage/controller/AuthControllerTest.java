package com.cloudstorage.controller;

import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.service.DirectoryService.DirectoryService;
import com.cloudstorage.service.ResourceService.FileService;
import com.cloudstorage.service.UserService.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageSource messageSource;

    @MockitoBean
    private SecurityFilterChain securityFilterChain;

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private DirectoryService directoryService;

    @MockitoBean
    private UserService userService;

    @Test
    void testRegistration_successfulRegistration() throws Exception {
        // Given
        UserPayload userPayload = new UserPayload("valid_username", "valid_password");

        // When + then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "valid_username",
                                    "password": "valid_password"
                                }
                                """)).andExpect(status().isCreated())
//                .andDo(print())
                .andExpect(content().string(containsString("valid_username")));

    }

    @Test
    void testRegistration_badRequestDueToInvalidUsername() throws Exception {
        // Given
        UserPayload userPayload = new UserPayload("valid_username", "valid_password");

        // When + then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "ll",
                                    "password": "valid_password"
                                }
                                """)).andExpect(status().isBadRequest())
//                .andDo(print())
                .andExpect(content().string(
                        containsString(this.messageSource.getMessage(
                                "users.errors.invalid_input", null, Locale.getDefault()))));

    }

    @Test
    void testRegistration_badRequestDueToInvalidPassword() throws Exception {
        // Given
        UserPayload userPayload = new UserPayload("valid_username", "valid_password");

        // When + then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "valid_username",
                                    "password": "lol"
                                }
                                """)).andExpect(status().isBadRequest())
//                .andDo(print())
                .andExpect(content().string(
                        containsString(this.messageSource.getMessage(
                                "users.errors.invalid_input", null, Locale.getDefault()))));

    }
}