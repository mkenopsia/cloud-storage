//package com.cloudstorage.controller;
//
//import com.cloudstorage.controller.payload.UserPayload;
//import com.cloudstorage.service.api.AuthService;
//import com.cloudstorage.service.api.UserService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.context.MessageSource;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.mockito.Mockito.doThrow;
//import static org.mockito.Mockito.verify;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(AuthController.class)
//@AutoConfigureMockMvc(addFilters = false)
//class AuthControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockitoBean
//    private MessageSource messageSource;
//
//    @MockitoBean
//    private UserService userService;
//
//    @MockitoBean
//    private AuthService authService;
//
//    private UserPayload userPayload;
//
//    @BeforeEach
//    void setUp() {
//        userPayload = new UserPayload("user_1", "password");
//    }
//
//    @Test
//    void testRegister_SuccessfulRegister() throws Exception {
//
//        mockMvc.perform(post("/api/auth/sign-up")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content("""
//                        {
//                              "username": "user_1",
//                              "password": "password"
//                        }
//                        """))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.username").value("user_1"));
//
//
//        verify(userService).save(userPayload);
//    }
//
//    @Test
//    void testRegister_UserAlreadyExists() throws Exception {
//        doThrow(new IllegalStateException("users.errors.user_already_exists")).when(userService).save(userPayload);
//
//
//        mockMvc.perform(post("/api/auth/sign-up")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("""
//                        {
//                              "username": "user_1",
//                              "password": "password"
//                        }
//                        """))
//                .andExpect(status().isConflict());
//    }
//
//    @Test
//    void testRegister_NameValidationError() throws Exception {
//
//        mockMvc.perform(post("/api/auth/sign-up")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("""
//                        {
//                              "username": "u",
//                              "password": "password"
//                        }
//                        """))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.message").isNotEmpty());
//    }
//
//    @Test
//    void testRegister_PasswordValidationError() throws Exception {
//
//        mockMvc.perform(post("/api/auth/sign-up")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("""
//                        {
//                              "username": "user",
//                              "password": "pwd"
//                        }
//                        """))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.message").isNotEmpty());
//    }
//
//    @Test
//    void testLogin_SuccessfulLogin() throws Exception {
//
//        mockMvc.perform(post("/api/auth/sign-in")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("""
//                        {
//                              "username": "user_1",
//                              "password": "password"
//                        }
//                        """))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.username").value("user_1"));
//
//
//        verify(authService).loginUser(userPayload);
//    }
//
//    @Test
//    void testLogin_NameValidationError() throws Exception {
//
//        mockMvc.perform(post("/api/auth/sign-in")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("""
//                        {
//                              "username": "u",
//                              "password": "password"
//                        }
//                        """))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.message").isNotEmpty());
//    }
//
//    @Test
//    void testLogin_PasswordValidationError() throws Exception {
//
//        mockMvc.perform(post("/api/auth/sign-in")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("""
//                        {
//                              "username": "user",
//                              "password": "pwd"
//                        }
//                        """))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.message").isNotEmpty());
//    }
//
//    @Test
//    void testLogin_InvalidData() throws Exception {
//        doThrow(new IllegalArgumentException("users.errors.invalid_password")).when(authService).loginUser(userPayload);
//
//
//        mockMvc.perform(post("/api/auth/sign-in")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("""
//                        {
//                              "username": "user_1",
//                              "password": "password"
//                        }
//                        """))
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    void testLogout_SuccessLogout() throws Exception {
//        mockMvc.perform(post("/api/auth/sign-out")).andExpect(status().isNoContent());
//    }
//}