//package com.cloudstorage.service;
//
//import com.cloudstorage.controller.payload.UserPayload;
//import com.cloudstorage.entity.User;
//import com.cloudstorage.repository.UserRepository;
//import com.cloudstorage.service.impl.DefaultAuthService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class DefaultAuthServiceTest {
//
//    @Mock
//    private UserRepository userRepository;
//    @Mock
//    private BCryptPasswordEncoder passwordEncoder;
//
//    @InjectMocks
//    private DefaultAuthService authService;
//
//    private UserPayload userPayload;
//    private User mockUser;
//
//    @BeforeEach
//    void setUp() {
//        userPayload = new UserPayload("user", "password");
//        mockUser = new User();
//        mockUser.setUsername("user");
//        mockUser.setPassword("encodedPassword");
//    }
//
//    @Test
//    void testLoginUser_SuccessfulAuthentication() {
//        when(userRepository.getByUsername(userPayload.username())).thenReturn(Optional.ofNullable(mockUser));
//        when(passwordEncoder.matches(userPayload.password(), mockUser.getPassword())).thenReturn(true);
//
//        authService.loginUser(userPayload);
//
//        verify(userRepository).getByUsername(userPayload.username());
//        verify(passwordEncoder).matches(userPayload.password(), mockUser.getPassword());
//    }
//
//    @Test
//    void testLoginUser_InvalidPassword() {
//        when(userRepository.getByUsername(userPayload.username())).thenReturn(Optional.ofNullable(mockUser));
//        when(passwordEncoder.matches(userPayload.password(), mockUser.getPassword())).thenReturn(false);
//
//
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
//                () -> authService.loginUser(userPayload));
//
//
//        assertEquals("users.errors.invalid_password", exception.getMessage());
//
//
//        verify(userRepository).getByUsername(userPayload.username());
//        verify(passwordEncoder).matches(userPayload.password(), mockUser.getPassword());
//    }
//
//    @Test
//    void testLoginUser_UserNotFound() {
//        when(userRepository.getByUsername(userPayload.username())).thenReturn(null);
//
//
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
//                () -> authService.loginUser(userPayload));
//
//
//        assertEquals("users.errors.not_found", exception.getMessage());
//
//
//        verify(userRepository).getByUsername(userPayload.username());
//    }
//}