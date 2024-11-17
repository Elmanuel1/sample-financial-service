package com.spherelabs.validators.transactions;

import com.spherelabs.model.Transaction;

public interface TransactionValidator {

    /**
     * Validates the transaction as a whole, applying necessary business rules.
     *
     * @param transaction The transaction to validate.
     * @return true if the transaction passes validation; otherwise, false.
     */
    boolean validate(Transaction transaction);
}
