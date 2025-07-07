package com.cloudstorage.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.Locale;

@ControllerAdvice
@RequiredArgsConstructor
public class ResourceControllerAdvice {

    private final MessageSource messageSource;

    @ExceptionHandler(MissingServletRequestParameterException.class)
    private ResponseEntity<ProblemDetail> handleBlankPathCase(MissingServletRequestParameterException exception, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setProperty("message",
                this.messageSource.getMessage("validation.error.path.path_is_missing", null, "error", locale));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(FileAlreadyExistsException.class)
    private ResponseEntity<ProblemDetail> handleFileAlreadyExistsCase(FileAlreadyExistsException exception, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setProperty("message",
                this.messageSource.getMessage(exception.getReason(), new Object[] {exception.getFile(), exception.getOtherFile()}, "error", locale));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
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
