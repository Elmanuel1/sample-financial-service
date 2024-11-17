package com.spherelabs.processors;

import com.spherelabs.config.AppConfiguration;
import com.spherelabs.error.Failure;
import com.spherelabs.error.FailureCode;
import com.spherelabs.model.Transaction;
import com.spherelabs.repository.LedgerRepository;
import com.spherelabs.repository.TransactionRepository;
import com.spherelabs.services.LiquidityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementProcessor {
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final LiquidityService liquidityService;
    private final AppConfiguration appConfiguration;


    @Scheduled(fixedRate = 1000) // Run every second
    public void processSettlements() {
        transactionRepository.getSettlementEligibleTransactions(OffsetDateTime.now().minusSeconds(1), appConfiguration.getSettlementPollSize())
                .peekLeft(failure -> log.error("Failed to get settlement eligible transactions. Reason: {}", failure.message(), failure.cause()))
                .peek(transactions -> transactions.forEach(this::process));
    }

    private void process(Transaction transaction) {
        switch (transaction.getStatus()) {
            case FUNDS_LOCKED:
                log.warn("Transaction not in a completed state. Transaction: {}", transaction);
                processLockedTransaction(transaction);
                break;
            case FAILED:
                log.info("Transaction has failed. Returning funds to pool. Transaction: {}", transaction);
                returnFundsToPool(transaction, Transaction.Status.FAILED, Transaction.Status.FUNDS_LOCKED, "Transaction failed");
                break;
            case COMPLETED:
                log.info("Transaction has been completed. Skipping processing. Transaction: {}", transaction);
                settle(transaction);
                break;
            case REQUIRE_INTERVENTION:
                log.warn("Transaction requires intervention. Another job handle these types. Transaction: {}", transaction);
                break;
            default:
                log.error("Unknown transaction status. Skipping processing. Transaction: {}", transaction);
        }
    }

    private void processLockedTransaction(Transaction transaction) {
        if (transaction.getUnlockedId() != null) {
            log.warn("Anomaly. Transaction has already been unlocked. Transaction: {}. Unlock id: {}", transaction.getId(), transaction.getUnlockedId());
            transactionRepository.markSettlementStatus(transaction.getId(), "Amount unlocked does not match expected amount", Transaction.SettlementStatus.REQUIRE_INTERVENTION, Transaction.Status.EXPIRED, Transaction.Status.FUNDS_LOCKED)
                    .peekLeft(failure -> log.error("Failed to mark transaction as expired. Reason: {}", failure.message(), failure.cause()));
        } else {
            log.warn("Transaction is not in a completed state. Transaction: {}", transaction);
            returnFundsToPool(transaction, Transaction.Status.EXPIRED, Transaction.Status.FUNDS_LOCKED, "Transaction failed");
        }
    }

    // although in real world, we would normally decouple this two. Marking as failed.  Then have another job to return the funds to the pool
    private void returnFundsToPool(Transaction transaction, Transaction.Status newTransaction, Transaction.Status oldTransactionStatus,  String message) {
        liquidityService.unlockBalance(transaction.getLockedId())
                .flatMap(__ -> transactionRepository.markSettlementStatus(transaction.getId(), message, Transaction.SettlementStatus.SETTLEMENT_STOPPED, newTransaction, oldTransactionStatus))
                .peekLeft(failure -> log.error("Failed to return locked funds. Reason: {}", failure.message(), failure.cause()))
                .peekLeft(failure -> handleFailureUnlockingFunds(transaction.getId(), failure));
    }

    private void handleFailureUnlockingFunds(Long transactionId, Failure failure) {
        if (failure.code().equals(FailureCode.NOT_FOUND.getCode())) {
            // weird we cannt unlock funds. We would have to investigate this further
            log.error("Failed to unlock funds. Funds not found. Reason: {}", failure.message());
            transactionRepository.markSettlementStatus(transactionId, "weird but the lock id is not found", Transaction.SettlementStatus.REQUIRE_INTERVENTION, Transaction.Status.FUNDS_LOCKED)
                    .peekLeft(failure1 -> log.error("Failed to mark transaction as failed. Reason: {}", failure1.message(), failure1.cause()));
        } else {
            incrementSettlementCount(transactionId);
        }
    }

    private void incrementSettlementCount(Long transactionId) {
        transactionRepository.incrementSettlementRetryCount(transactionId)
                .peekLeft(failure1 -> log.error("Failed to increment status retry count. Reason: {}", failure1.message(), failure1.cause()));
    }

    private void settle(Transaction transaction) {
        log.warn("Transaction with lock status has already been unlocked. Transaction: {}. Unlock id {}", transaction.getId(), transaction.getUnlockedId());
        // not sure why we would have a transaction that has been unlocked but not completed
        var lockEntry = ledgerRepository.getByLockId(transaction.getUnlockedId())
                .peekLeft(failure -> log.error("Failed to get ledger entry for unlock id: {}. Reason: {}", transaction.getUnlockedId(), failure.message(), failure.cause()));
        if (lockEntry.isLeft()) {
            // if there is an error getting the unlock entry, we can't proceed. Hopefully another seperate process will pick this up
            log.error("Failed to get unlock entry for transaction: {}. Reason: {}", transaction, lockEntry.getLeft().message(), lockEntry.getLeft().cause());
            return;
        }

        var entry = lockEntry.get();
        //we want to compare the amount that was unlocked to the amount that was expected to be unlocked just to be sure
        if (entry.amount().compareTo(transaction.getToAmount()) != 0 || !entry.currencyCode().equals(transaction.getToCurrency())) {
            log.error("Amount unlocked does not match expected amount. Transaction: {}. Ledger Entry: {}", transaction, entry);
            // we want to investigate this further. We would have a job to check this manually
            transactionRepository.markSettlementStatus(transaction.getId(), "Amount unlocked does not match expected amount", Transaction.SettlementStatus.REQUIRE_INTERVENTION, transaction.getStatus())
                    .peekLeft(failure -> log.error("Failed to mark transaction as failed. Reason: {}", failure.message(), failure.cause()));
            return;
        }

        // at settlement time, we want to transfer the
        liquidityService.debitLockedBalance(transaction.getUnlockedId())
                .flatMap(__ -> transactionRepository.markSuccessfulSettlementStatus(transaction.getId(), Transaction.Status.COMPLETED))
                .peekLeft(failure -> log.error("Failed to transfer unlocked funds. Reason: {}", failure.message(), failure.cause()));
    }
}
