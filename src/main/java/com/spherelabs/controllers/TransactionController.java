package com.spherelabs.controllers;

import com.spherelabs.error.ApplicationException;
import com.spherelabs.model.api.TransferRequest;
import com.spherelabs.model.api.TransferResponse;
import com.spherelabs.services.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService service;

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.OK)
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        return service.transfer(request)
                .map(r -> new TransferResponse(r.getInternalTransferId(), r.getStatus(), r.getCreatedAt(), r.getUpdatedAt(), r.getFailureReason()))
                .getOrElseThrow(failure -> {
                    throw new ApplicationException(failure);
                });
    }
}
