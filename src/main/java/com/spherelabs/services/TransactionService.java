package com.spherelabs.services;

import com.spherelabs.error.Failure;
import com.spherelabs.model.Transaction;
import com.spherelabs.model.api.TransferRequest;
import io.vavr.control.Either;

import java.util.List;

public interface TransactionService {

    /**
     * Transfer money from one currency account to another account
     * @param request Transfer request
     * @return failure or the transaction
     */
    Either<Failure, Transaction> transfer(TransferRequest request);


}
