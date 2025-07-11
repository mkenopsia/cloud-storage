package com.cloudstorage.controller;

import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.service.DirectoryService.DirectoryService;
import com.cloudstorage.service.UserService.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.NoSuchFileException;
import java.util.Map;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class AuthController {

    private final UserService userService;
    private final DirectoryService directoryService;

    @PostMapping("/auth/sign-up")
    public ResponseEntity<?> register(@Valid @RequestBody UserPayload userPayload,
                                      BindingResult bindingResult) throws BindException, NoSuchFileException {
        if(bindingResult.hasErrors()) {
            System.out.println(bindingResult.getAllErrors().stream().map(ObjectError::getDefaultMessage).toList());
            throw new BindException(bindingResult);
        }

        this.userService.save(userPayload);
        this.directoryService.createRootDirectory(userPayload);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("username", userPayload.username()));
    }
}
