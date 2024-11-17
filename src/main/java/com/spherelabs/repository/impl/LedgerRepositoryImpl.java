package com.spherelabs.repository.impl;

import com.assetiq.jooq.tables.records.LedgerRecord;
import com.spherelabs.error.Failure;
import com.spherelabs.model.Ledger;
import com.spherelabs.model.Transaction;
import com.spherelabs.repository.LedgerRepository;
import com.spherelabs.utils.Eithers;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.RecordMapper;
import org.springframework.stereotype.Repository;

import static com.assetiq.jooq.Tables.LEDGER;

@RequiredArgsConstructor
@Repository
@Slf4j
public class LedgerRepositoryImpl implements LedgerRepository {
    private final DSLContext dslContext;
    private static final RecordMapper<LedgerRecord, Ledger> MAPPER = record -> new Ledger(
            record.getId(),
            record.getTransactionId(),
            record.getCurrencyCode(),
            Transaction.Type.from(record.getTransactionType()),
            record.getAmount(),
            record.getCreatedAt(),
            record.getDescription()
    );


    @Override
    public Either<Failure, Ledger> getByLockId(Long lockId) {
        return Eithers.of(() -> dslContext.selectFrom(LEDGER)
                .where(LEDGER.ID.eq(lockId))
                .and(LEDGER.TRANSACTION_TYPE.eq(Transaction.Type.LOCK.getValue()))
                .fetchSingle(MAPPER));
    }
}
