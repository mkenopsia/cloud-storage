package com.cloudstorage.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;

import java.nio.file.FileAlreadyExistsException;
import java.util.Locale;

@ControllerAdvice
@RequiredArgsConstructor
public class FileControllerAdvice {

    private final MessageSource messageSource;

    @ExceptionHandler(FileAlreadyExistsException.class)
    private ResponseEntity<ProblemDetail> HandleFileAlreadyExistsCase(FileAlreadyExistsException exception, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setProperty("message",
                this.messageSource.getMessage(exception.getReason(), new Object[] {exception.getFile(), exception.getOtherFile()}, "error", locale));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    private ResponseEntity<ProblemDetail> HandleBlankFilesCase(IllegalArgumentException exception, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setProperty("message",
                this.messageSource.getMessage(exception.getMessage(), null, "error", locale));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<String> handleFileSizeLimitExceeded() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body("Размер загружаемых файлов слишком большой"); //TODO: потестить - надо или нет.. и расхардкодить
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Неверный тип данных для параметра 'files'");
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<String> handleNullFiles() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Файлы не были переданы");
    }
}
