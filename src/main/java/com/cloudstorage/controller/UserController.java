package com.cloudstorage.controller;

import com.cloudstorage.service.AuthService.DefaultAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final DefaultAuthService authService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserUsername() {
        return ResponseEntity.ok().body(authService.getUsernameFromSession());
    }
}
