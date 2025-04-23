package com.cloudstorage.service;

import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.entity.User;
import com.cloudstorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultAuthService implements AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void loginUser(UserPayload userPayload) {
        User user = userRepository.getByUsername(userPayload.username());

        if(!passwordEncoder.matches(userPayload.password(), user.getPassword())) {
            throw new IllegalArgumentException("users.errors.invalid_password");
        }
    }
}
