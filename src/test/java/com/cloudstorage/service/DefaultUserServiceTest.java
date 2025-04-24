package com.cloudstorage.service;

import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.entity.User;
import com.cloudstorage.repository.UserRepository;
import com.cloudstorage.service.impl.DefaultUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private DefaultUserService userService;

    private UserPayload userPayload;
    private User mockUser;

    @BeforeEach
    void setUp() {
        userPayload = new UserPayload("test", "password");
        mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("test");
        mockUser.setPassword("password");
    }

    @Test
    void testSave_UserDoesNotExist() {
        when(userRepository.existsByUsername(userPayload.username())).thenReturn(false);
        when(passwordEncoder.encode(userPayload.password())).thenReturn("encodedPassword");


        userService.save(userPayload);


        verify(userRepository).existsByUsername(userPayload.username());
        verify(passwordEncoder).encode(userPayload.password());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testSave_UserAlreadyExists() {
        when(userRepository.existsByUsername(userPayload.username())).thenReturn(true);


        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> userService.save(userPayload));


        assertEquals("users.errors.user_already_exists", exception.getMessage());

        verify(userRepository).existsByUsername(userPayload.username());
    }


}