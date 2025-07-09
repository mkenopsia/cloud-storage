package com.cloudstorage.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Locale;

@ControllerAdvice
@RequiredArgsConstructor
public class AuthControllerAdvice {

    private final MessageSource messageSource;

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ProblemDetail> handleValidationError(BindException exception, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                this.messageSource.getMessage("users.errors.invalid_input", null, "users.errors.invalid_input", locale));

        problemDetail.setProperty("errors", exception.getAllErrors().stream()
                .map(error -> messageSource.getMessage(error.getDefaultMessage(), null, "error", locale))
                .toList());
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
