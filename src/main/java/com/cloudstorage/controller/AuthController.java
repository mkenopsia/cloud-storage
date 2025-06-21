package com.cloudstorage.controller;

import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.service.UserService.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/auth/sign-up")
    public ResponseEntity<?> register(@RequestBody @Valid UserPayload userPayload,
                                      BindingResult bindingResult) throws BindException {
        if(bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }

        this.userService.save(userPayload);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("username", userPayload.username()));
    }
}
