package com.cloudstorage.controller;

import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.controller.payload.UsernamePayload;
import com.cloudstorage.service.DirectoryService.DirectoryService;
import com.cloudstorage.service.UserService.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.NoSuchFileException;
import java.util.Map;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Tag(name = "Контроллер регистрации")
public class RegisterController {

    private final UserService userService;
    private final DirectoryService directoryService;

    @PostMapping("/auth/sign-up")
    @Operation(summary = "Регистрация пользователя", responses = {
            @ApiResponse(responseCode = "201", description = "Пользователь создан", content = @Content(schema = @Schema(implementation = UsernamePayload.class))),
            @ApiResponse(responseCode = "400", description = "Невалидные данные", content = @Content),
            @ApiResponse(responseCode = "409", description = "Пользователь с таким именем уже существует", content = @Content),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка", content = @Content)
    })
    public ResponseEntity<?> register(@Valid @RequestBody UserPayload userPayload,
                                      BindingResult bindingResult) throws BindException, NoSuchFileException {
        if(bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }

        this.userService.save(userPayload);
        this.directoryService.createUserRootDirectory(userPayload);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("username", userPayload.username()));
    }
}
