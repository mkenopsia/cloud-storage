package com.cloudstorage.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.nio.file.NoSuchFileException;
import java.util.Locale;

@ControllerAdvice
@RequiredArgsConstructor
public class CommonControllerAdvice {

    private final MessageSource messageSource;

    @ExceptionHandler(RuntimeException.class)
    private ResponseEntity<ProblemDetail> handleUnexpectedException(RuntimeException exception, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setProperty("message",
                this.messageSource.getMessage("server.error.internal_server_error", null, "error", locale));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    @ExceptionHandler(NoSuchFileException.class)
    private ResponseEntity<ProblemDetail> handleFileNotFoundCase(NoSuchFileException exception, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setProperty("message",
                this.messageSource.getMessage(exception.getMessage(), null, "error", locale));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    private ResponseEntity<ProblemDetail> handleResourceAlreadyExistsCase(UnsupportedOperationException exception, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setProperty("message",
                this.messageSource.getMessage(exception.getMessage(), null, "error", locale));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    private ResponseEntity<ProblemDetail> handleBlankFilesCase(IllegalArgumentException exception, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setProperty("message",
                this.messageSource.getMessage(exception.getMessage(), null, "error", locale));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }
}
