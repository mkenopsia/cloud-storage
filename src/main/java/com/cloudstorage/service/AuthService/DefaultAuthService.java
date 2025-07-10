package com.cloudstorage.service.AuthService;

import com.cloudstorage.controller.payload.UsernamePayload;
import com.cloudstorage.entity.User;
import com.cloudstorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultAuthService implements AuthService {

    private final UserRepository userRepository;

    @Override
    public UsernamePayload getUsernameFromSession() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return new UsernamePayload(username);
    }

    @Override
    public Integer getUserIdFromSession() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = this.userRepository.getByUsername(username).orElseThrow(() -> new UsernameNotFoundException("users.errors.not_found"));
        return user.getId();
    }
}
