package com.fonestore.staff_api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> notFound(NotFoundException e) {
        log.warn("NotFound: {}", e.getMessage(), e);
        Map<String,Object> body = new HashMap<>();
        body.put("error", e.getMessage() != null ? e.getMessage() : "Not Found");
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> badRequest(BadRequestException e) {
        log.warn("BadRequest: {}", e.getMessage(), e);
        Map<String,Object> body = new HashMap<>();
        body.put("error", e.getMessage() != null ? e.getMessage() : "Bad Request");
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> other(Exception e) {
        // Log toàn bộ stacktrace để debug nguyên nhân gốc
        log.error("Unhandled exception: {}", e.getMessage(), e);

        Map<String,Object> body = new HashMap<>();
        String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        body.put("error", msg);
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("timestamp", Instant.now().toString());
        // Không để null values => không dùng Map.of khi giá trị có thể null
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
