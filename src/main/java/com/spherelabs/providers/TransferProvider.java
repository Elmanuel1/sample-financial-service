package com.spherelabs.providers;


import com.spherelabs.error.Failure;
import com.spherelabs.model.Transaction;
import io.vavr.control.Either;

public interface TransferProvider {
    // dummy implementation
    // retruns etither failed or success
    Either<Failure, Transaction.Status> transfer(Transaction transaction);
}
