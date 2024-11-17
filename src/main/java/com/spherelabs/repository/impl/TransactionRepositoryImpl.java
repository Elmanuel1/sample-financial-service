package com.spherelabs.repository.impl;

import com.assetiq.jooq.enums.TransactionStatus;
import com.assetiq.jooq.tables.records.TransactionRecord;
import com.spherelabs.error.Failure;
import com.spherelabs.model.Transaction;
import com.spherelabs.repository.TransactionRepository;
import com.spherelabs.utils.Eithers;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.RecordMapper;
import org.jooq.impl.DSL;
import org.jooq.types.YearToSecond;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

import static com.assetiq.jooq.Tables.FAILED_TRANSACTION_EVENT;
import static com.assetiq.jooq.Tables.TRANSACTION;

@RequiredArgsConstructor
@Repository
@Slf4j
public class TransactionRepositoryImpl implements TransactionRepository {
    private final DSLContext dsl;
    private static final RecordMapper<TransactionRecord, Transaction> MAPPER = record -> {
        Transaction transaction = new Transaction();
        transaction.setId(record.getId());
        transaction.setTransferId(record.getTransferId());
        transaction.setInternalTransferId(record.getInternalTransferId());
        transaction.setSenderAccount(record.getSenderAccount());
        transaction.setReceiverAccount(record.getReceiverAccount());
        transaction.setFromAmount(record.getFromAmount());
        transaction.setFromCurrency(record.getFromCurrency());
        transaction.setToCurrency(record.getToCurrency());
        transaction.setToAmount(record.getToAmount());
        transaction.setMargin(record.getMargin());
        transaction.setMarginCurrency(record.getMarginCurrency());
        transaction.setMarginRate(record.getMarginRate());
        transaction.setFxRate(record.getFxRate());
        transaction.setEffectiveRateDate(record.getRateEffectiveDate());
        transaction.setStatus(Transaction.Status.from(record.getStatus().name()));
        transaction.setScheduledSettlementTime(record.getScheduledSettlementTime());
        transaction.setSettlementWindow(record.getSettlementWindow().toDuration());
        transaction.setSettlementAttempts(record.getSettlementAttempts());
        transaction.setActualSettlementTime(record.getActualSettlementTime());
        transaction.setCreatedAt(record.getCreatedAt());
        transaction.setDescription(record.getDescription());
        transaction.setUpdatedAt(record.getUpdatedAt());
        transaction.setLockedId(record.getLockedId());
        transaction.setFailureReason(record.getFailureReason());

        return transaction;
    };

    @Override
    public Either<Failure, Transaction> insertNew(Transaction transaction) {
        return Eithers.of(() -> dsl.insertInto(TRANSACTION)
                .set(TRANSACTION.TRANSFER_ID, transaction.getTransferId())
                .set(TRANSACTION.INTERNAL_TRANSFER_ID, transaction.getInternalTransferId())
                .set(TRANSACTION.SENDER_ACCOUNT, transaction.getSenderAccount())
                .set(TRANSACTION.RECEIVER_ACCOUNT, transaction.getReceiverAccount())
                .set(TRANSACTION.FROM_AMOUNT, transaction.getFromAmount())
                .set(TRANSACTION.TO_AMOUNT, transaction.getToAmount())
                .set(TRANSACTION.FROM_CURRENCY, transaction.getFromCurrency())
                .set(TRANSACTION.TO_CURRENCY, transaction.getToCurrency())
                .set(TRANSACTION.MARGIN, transaction.getMargin())
                .set(TRANSACTION.MARGIN_CURRENCY, transaction.getMarginCurrency())
                .set(TRANSACTION.FX_RATE, transaction.getFxRate())
                .set(TRANSACTION.RATE_EFFECTIVE_DATE, transaction.getEffectiveRateDate())
                .set(TRANSACTION.MARGIN_RATE, transaction.getMarginRate())
                .set(TRANSACTION.STATUS, DSL.val(transaction.getStatus().getValue(), TRANSACTION.STATUS))
                .set(TRANSACTION.SCHEDULED_SETTLEMENT_TIME, transaction.getScheduledSettlementTime())
                .set(TRANSACTION.SETTLEMENT_WINDOW, YearToSecond.valueOf(transaction.getSettlementWindow()))
                .set(TRANSACTION.ACTUAL_SETTLEMENT_TIME, transaction.getActualSettlementTime())
                .set(TRANSACTION.CREATED_AT, transaction.getCreatedAt())
                .set(TRANSACTION.DESCRIPTION, transaction.getDescription())
                .returning()
                .fetchSingle(MAPPER))
                .peekLeft(failure -> log.error("Failed to insert new transaction", failure.cause()));
    }

    public Either<Failure, Transaction> recordFailedEvent(Transaction transaction) {
        return Eithers.of(() -> dsl.insertInto(FAILED_TRANSACTION_EVENT)
                        .set(FAILED_TRANSACTION_EVENT.TRANSFER_ID, transaction.getTransferId())
                        .set(FAILED_TRANSACTION_EVENT.SENDER_ACCOUNT, transaction.getSenderAccount())
                        .set(FAILED_TRANSACTION_EVENT.RECEIVER_ACCOUNT, transaction.getReceiverAccount())
                        .set(FAILED_TRANSACTION_EVENT.FROM_AMOUNT, transaction.getFromAmount())
                        .set(FAILED_TRANSACTION_EVENT.FROM_CURRENCY, transaction.getFromCurrency())
                        .set(FAILED_TRANSACTION_EVENT.TO_CURRENCY, transaction.getToCurrency())
                        .set(TRANSACTION.DESCRIPTION, transaction.getDescription())
                        .execute())
                .map(i -> transaction)
                .peekLeft(failure -> log.error("Failed to insert new transaction", failure.cause()));
    }

    @Override
    public Either<Failure, List<Transaction>> getSettlementEligibleTransactions(int limit) {
        return Eithers.of(() -> dsl.selectFrom(TRANSACTION)
                .where(TRANSACTION.SETTLEMENT_STATUS.isNull())
                .and(TRANSACTION.SCHEDULED_SETTLEMENT_TIME.lessOrEqual(OffsetDateTime.now()))
                .limit(limit)
                .forUpdate()
                .skipLocked()
                .fetch(MAPPER));

    }

    @Override
    public Either<Failure, Transaction> markSettlementStatus(Long id, String message, Transaction.SettlementStatus newStatus, Transaction.Status transactionStatus, Transaction.Status oldStatus) {
        return Eithers.of(() -> dsl.update(TRANSACTION)
                .set(TRANSACTION.SETTLEMENT_STATUS, newStatus.getValue())
                .set(TRANSACTION.SETTLEMENT_MESSAGE, message)
                .set(TRANSACTION.STATUS, TransactionStatus.valueOf(transactionStatus.getValue()))
                .set(TRANSACTION.UPDATED_AT, OffsetDateTime.now())
                .where(TRANSACTION.ID.eq(id)
                .and(TRANSACTION.STATUS.eq(TransactionStatus.valueOf(oldStatus.getValue()))))
                .returning()
                .fetchSingle(MAPPER));
    }

    @Override
    public Either<Failure, Transaction> markSettlementStatus(Long id, String message, Transaction.SettlementStatus newStatus, Transaction.Status oldStatus) {
        return Eithers.of(() -> dsl.update(TRANSACTION)
                .set(TRANSACTION.SETTLEMENT_STATUS, newStatus.getValue())
                .set(TRANSACTION.SETTLEMENT_MESSAGE, message)
                .set(TRANSACTION.UPDATED_AT, OffsetDateTime.now())
                .where(TRANSACTION.ID.eq(id)
                .and(TRANSACTION.STATUS.eq(TransactionStatus.valueOf(oldStatus.getValue()))))
                .returning()
                .fetchSingle(MAPPER));
    }

    @Override
    public Either<Failure, Transaction> markSuccessfulSettlementStatus(Long id, Transaction.Status oldStatus) {
        return Eithers.of(() -> dsl.update(TRANSACTION)
                .set(TRANSACTION.SETTLEMENT_STATUS, Transaction.SettlementStatus.SETTLED.getValue())
                .set(TRANSACTION.UPDATED_AT, OffsetDateTime.now())
                .set(TRANSACTION.ACTUAL_SETTLEMENT_TIME,  OffsetDateTime.now())
                .where(TRANSACTION.ID.eq(id)
                        .and(TRANSACTION.STATUS.eq(TransactionStatus.valueOf(oldStatus.getValue()))))
                .returning()
                .fetchSingle(MAPPER));
    }

    @Override
    public Either<Failure, Void> incrementSettlementRetryCount(Long transactionId) {
        return Eithers.of(() -> dsl.update(TRANSACTION)
                .set(TRANSACTION.SETTLEMENT_ATTEMPTS, TRANSACTION.SETTLEMENT_ATTEMPTS.plus(1))
                .set(TRANSACTION.UPDATED_AT, OffsetDateTime.now())
                .where(TRANSACTION.ID.eq(transactionId))
                .execute())
                .map(__ -> null);
    }

    @Override
    public Either<Failure, Transaction> updateLockStatus(Long id, Long lockId, Transaction.Status newStatus, Transaction.Status oldStatus) {
        return Eithers.of(() -> dsl.update(TRANSACTION)
                .set(TRANSACTION.STATUS, DSL.val(newStatus.getValue(), TRANSACTION.STATUS))
                .set(TRANSACTION.LOCKED_ID, lockId)
                .set(TRANSACTION.UPDATED_AT, OffsetDateTime.now())
                .where(TRANSACTION.ID.eq(id)
                        .and(TRANSACTION.STATUS.eq(TransactionStatus.valueOf(oldStatus.getValue()))))
                .returning()
                .fetchSingle(MAPPER));
    }

    @Override
    public Either<Failure, Transaction> updateStatus(Long id, String  message, Transaction.Status newStatus, Transaction.Status oldStatus) {
        return Eithers.of(() -> dsl.update(TRANSACTION)
                .set(TRANSACTION.STATUS, DSL.val(newStatus.getValue(), TRANSACTION.STATUS))
                .set(TRANSACTION.FAILURE_REASON, message)
                .set(TRANSACTION.UPDATED_AT, OffsetDateTime.now())
                .where(TRANSACTION.ID.eq(id)
                        .and(TRANSACTION.STATUS.eq(TransactionStatus.valueOf(oldStatus.getValue()))))
                .returning()
                .fetchSingle(MAPPER));
    }

    @Override
    public Either<Failure, Transaction> getTransaction(String transactionId) {
        return Eithers.of(() -> dsl.selectFrom(TRANSACTION)
                .where(TRANSACTION.INTERNAL_TRANSFER_ID.eq(transactionId))
                .fetchSingle(MAPPER));
    }

    public Either<Failure, Transaction> getTransaction(String referenceId, String fromCurrency, String toCurrency) {
        return Eithers.of(() -> dsl.selectFrom(TRANSACTION)
                .where(TRANSACTION.TRANSFER_ID.eq(referenceId))
                .and(TRANSACTION.FROM_CURRENCY.eq(fromCurrency))
                .and(TRANSACTION.TO_CURRENCY.eq(toCurrency))
                .fetchSingle(MAPPER));
    }
}
