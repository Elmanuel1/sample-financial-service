package com.spherelabs.repository;

import com.spherelabs.error.Failure;
import com.spherelabs.model.Transaction;
import io.vavr.control.Either;

import java.util.List;


public interface TransactionRepository {

    /**
     * Insert a new transaction. Status is set to INITIATED
     * @param transaction The transaction to insert
     * @return Either a Failure or the inserted transaction
     */
    Either<Failure, Transaction> insertNew(Transaction transaction);

    /**
     * Update the status of a transaction
     * @param transactionId The transaction id
     * @param lockId The lock id
     * @param status The new status
     * @param oldStatus The old status
     * @return Either a Failure or the updated transaction
     */
    Either<Failure, Transaction> updateLockStatus(Long transactionId, Long lockId, Transaction.Status status,  Transaction.Status oldStatus);

    /**
     * Update the status of a transaction
     * @param transactionId The transaction id
     * @param message The reason for failure
     * @param status The new status
     * @param oldStatus The old status
     * @return Either a Failure or the updated transaction
     */
    Either<Failure, Transaction> updateStatus(Long transactionId, String message ,Transaction.Status status,  Transaction.Status oldStatus);
    /**
     * Get a transaction by id
     * @param transactionId The transaction id
     * @return Either a Failure or the transaction
     */

    Either<Failure, Transaction> getTransaction(String transactionId, String fromCurrency, String toCurrency);
    Either<Failure, Transaction> getTransaction(String internalId);
    /**
     * Record a failed event. This is because they do not have constrained columns to be put in the transaction table
     * @param transaction The transaction
     * @return Either a Failure or true if the event was recorded
     */
    Either<Failure, Transaction> recordFailedEvent(Transaction transaction);

    /**
     * Get all transactions matching the query
     * This is npt paginated since it is a simple prototype
     * @param limit How many records should be fetched
     * @return Either a Failure or the list of transactions
     */

    Either<Failure, List<Transaction>> getSettlementEligibleTransactions(int limit);

    /**
     * Changes the status of a transaction and set the settlement status
     * @param id The transaction id
     * @param message The message to be recorded
     * @param newStatus The new status
     * @param oldStatus The old status
     * @return
     */
    Either<Failure, Transaction> markSettlementStatus(Long id, String message, Transaction.SettlementStatus newStatus, Transaction.Status transactionStatus, Transaction.Status oldStatus);

    /**
     * Mark the status of a settlement without changing the transaction status
     * @param id The transaction id
     * @param message The message to be recorded
     * @param settlementStatus The new settlement status
     * @param oldStatus The old status
     * @return
     */
    Either<Failure, Transaction> markSettlementStatus(Long id, String message, Transaction.SettlementStatus settlementStatus, Transaction.Status oldStatus);

    /**
     * Changes the settlement status to settled
     * @param id The transaction id
     * @param oldStatus The old status
     * @return
     */
    Either<Failure, Transaction> markSuccessfulSettlementStatus(Long id, Transaction.Status oldStatus);

    /**
     * Increment the settlement retry count
     * @param transactionId The transaction id
     * @return
     */
    Either<Failure, Void> incrementSettlementRetryCount(Long transactionId);
}
