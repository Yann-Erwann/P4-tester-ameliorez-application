package com.openclassrooms.starterjwt.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({NumberFormatException.class, BadRequestException.class})
    public ResponseEntity<Void> badRequest() {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Void> notFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<Void> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
