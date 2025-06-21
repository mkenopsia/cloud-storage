package com.cloudstorage.service.AuthService;

import com.cloudstorage.controller.payload.UsernamePayload;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultAuthService implements AuthService {

    @Override
    public UsernamePayload getUsernameFromSession() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return new UsernamePayload(username);
    }
}
