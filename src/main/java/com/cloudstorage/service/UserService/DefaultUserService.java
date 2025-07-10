package com.cloudstorage.service.UserService;

import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.entity.User;
import com.cloudstorage.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class DefaultUserService implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void save(UserPayload user) {
        if(userRepository.existsByUsername(user.username())) {
            throw new IllegalStateException("users.errors.user_already_exists");
        }
        User newUser = new User();
        newUser.setUsername(user.username());
        newUser.setPassword(passwordEncoder.encode(user.password()));
        this.userRepository.save(newUser);
    }

    @Override
    public Optional<User> findById(Integer id) {
        return this.userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return this.userRepository.findByUsername(username);
    }

    @Override
    public void deleteById(Integer id) {
        this.userRepository.deleteById(id);
    }

    @Override
    public boolean isAlreadyExists(UserPayload user) {
        return this.userRepository.existsByUsername(user.username());
    }
}
