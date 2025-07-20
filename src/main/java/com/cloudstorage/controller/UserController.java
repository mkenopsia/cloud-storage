package com.cloudstorage.controller;

import com.cloudstorage.controller.payload.UsernamePayload;
import com.cloudstorage.service.AuthService.AuthService;
import com.cloudstorage.service.AuthService.DefaultAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
@Tag(name = "Получение даных авторизованного пользователя")
public class UserController {

    private final AuthService authService;

    @GetMapping("/me")
    @Operation(summary = "Получение имени пользователя", responses = {
            @ApiResponse(responseCode = "200", description = "Успех", content = @Content(schema = @Schema(implementation = UsernamePayload.class))),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка", content = @Content)
    })
    public ResponseEntity<UsernamePayload> getCurrentUserUsername() {
        return ResponseEntity.ok().body(authService.getUsernameFromSession());
    }
}
