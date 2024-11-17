package com.spherelabs.controllers.advice;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.spherelabs.error.APIError;
import com.spherelabs.error.ApplicationException;
import com.spherelabs.error.ErrorTranslator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Collections;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GenericExceptionHandler {
    private final ObjectMapper objectMapper;

    @ExceptionHandler
    public ResponseEntity<Object> handle(MethodArgumentNotValidException ex) {
        HttpHeaders headers = new HttpHeaders();
        var err = ErrorTranslator.from(ex.getBindingResult());
        headers.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(MimeTypeUtils.APPLICATION_JSON_VALUE));
        return new ResponseEntity<>(err, headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<Object> handle(HttpMessageNotReadableException ex) {
        log.error("Error occurred processing the request", ex);
        HttpHeaders headers = new HttpHeaders();
        headers.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(MimeTypeUtils.APPLICATION_JSON_VALUE));
        return new ResponseEntity<>(ErrorTranslator.from(ex), headers, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(ApplicationException.class)
    @SneakyThrows
    public ResponseEntity<Object> handleCustomException(ApplicationException ex) {
        var error = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ex.getError());
        log.error("An application error occurred: {}", error, ex);
        return new ResponseEntity<>(ex.getError(), ex.getHttpStatus());
    }
}
