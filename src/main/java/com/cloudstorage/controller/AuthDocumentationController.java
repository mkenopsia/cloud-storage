package com.cloudstorage.controller;

import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.controller.payload.UsernamePayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Фильтр авторизации")
public class AuthDocumentationController {

    @PostMapping("/api/auth/sign-in")
    @Operation(summary = "Авторизация пользователя", responses = {
            @ApiResponse(responseCode = "200", description = "Успешная авторизация", content = @Content(schema = @Schema(implementation = UsernamePayload.class))),
            @ApiResponse(responseCode = "401", description = "Неверные данные (такого пользователя нет, или пароль неправильный)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка", content = @Content)
    })
    public ResponseEntity<?> authorizeUser(@RequestBody UserPayload userPayload) {
        return null;
    }
}
