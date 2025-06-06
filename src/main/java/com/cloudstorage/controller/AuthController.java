package com.cloudstorage.controller;

import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.service.api.AuthService;
import com.cloudstorage.service.api.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class AuthController {

    private final MessageSource messageSource;
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

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ProblemDetail> handleValidationError(BindException exception, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                this.messageSource.getMessage("users.errors.invalid_input", null, "users.errors.invalid_input", locale));

        problemDetail.setProperty("message", exception.getAllErrors().stream().map(ObjectError::getDefaultMessage));
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleUserAlreadyExistsCase(IllegalStateException exception, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setProperty("message",
                this.messageSource.getMessage(exception.getMessage(), null, "users.errors.user_already_exists", locale));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }
}
