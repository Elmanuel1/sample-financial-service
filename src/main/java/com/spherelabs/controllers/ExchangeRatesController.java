package com.spherelabs.controllers;

import com.spherelabs.error.ApplicationException;
import com.spherelabs.model.api.ExchangeRateRequest;
import com.spherelabs.services.ExchangeRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExchangeRatesController {
    private final ExchangeRateService service;

    @PostMapping("/fx-rate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<?> addRate(@Valid @RequestBody ExchangeRateRequest request) {
        return service.addRate(request.pair(), request.rate(), request.timestamp())
                .map(__ -> ResponseEntity.noContent().build())
                .getOrElseThrow(failure -> {
                    throw new ApplicationException(failure);
                });
    }
}
