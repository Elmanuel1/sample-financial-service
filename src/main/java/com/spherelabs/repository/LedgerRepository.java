package com.spherelabs.repository;

import com.spherelabs.error.Failure;
import com.spherelabs.model.Ledger;
import io.vavr.control.Either;

public interface LedgerRepository {
    /**
     * Get the ledger record by transaction id and currency code
     * @param lockId The lockId
     * @return Either a Failure or the ledger record
     */
    Either<Failure, Ledger> getByLockId(Long lockId);
}
