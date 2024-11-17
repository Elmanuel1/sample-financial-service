package com.spherelabs.providers;

import com.spherelabs.error.Failure;
import com.spherelabs.error.FailureCode;
import com.spherelabs.model.Transaction;
import io.vavr.control.Either;
import org.springframework.stereotype.Component;

@Component
public class DummyTransferProvider implements TransferProvider {
    @Override
    /**
     * Dummy implementation of transfer
     */
    public Either<Failure, Transaction.Status> transfer(Transaction transaction) {
        if (transaction.getSenderAccount().startsWith("111")) {
            return Either.right(Transaction.Status.FAILED);
        } else if (transaction.getSenderAccount().startsWith("222")) {
            return Either.right(Transaction.Status.PROCESSING);
        } else if (transaction.getSenderAccount().startsWith("333")) {
            return Either.left(Failure.from(FailureCode.UNKNOWN_ERROR));
        }
        return Either.right(Transaction.Status.COMPLETED);
    }
}
