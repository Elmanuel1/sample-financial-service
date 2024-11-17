package com.spherelabs.repository.impl;

import com.spherelabs.error.ApplicationException;
import com.spherelabs.error.Failure;
import com.spherelabs.error.FailureCode;
import com.spherelabs.model.LiquidityMovement;
import com.spherelabs.repository.LiquidityRepository;
import com.spherelabs.utils.Eithers;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static com.assetiq.jooq.Tables.*;

@RequiredArgsConstructor
@Repository
@Slf4j
public class LiquidityRepositoryImpl implements LiquidityRepository {
    private final DSLContext dslContext;

    @Override
    public Either<Failure, BigDecimal> getBalance(String currency) {
        return Eithers.of(() -> dslContext
                .select(LIQUIDITY_POOL.AVAILABLE_BALANCE)
                .from(LIQUIDITY_POOL)
                .where(LIQUIDITY_POOL.CURRENCY_CODE.eq(currency))
                .fetchOptional()
                .map(Record1::value1)
                .orElse(BigDecimal.ZERO));
    }

    @Override
    public Either<Failure, Long> lockBalance(
            LiquidityMovement liquidityMovement
    ) {
        return Eithers.of(() -> dslContext.transactionResult(config -> {
            
            var balance = DSL.using(config)
                    .select(
                            LIQUIDITY_POOL.AVAILABLE_BALANCE
                    )
                    .from(LIQUIDITY_POOL)
                    .where(LIQUIDITY_POOL.CURRENCY_CODE.eq(liquidityMovement.currencyCode()))
                    .forUpdate()
                    .fetchOptional()
                    .map(Record1::value1)
                    .orElse(BigDecimal.ZERO);
            
            if (balance.compareTo(liquidityMovement.amount().add(liquidityMovement.margin())) <= 0) {
                throw new ApplicationException(FailureCode.INSUFFICIENT_FUNDS);
            }

            DSL.using(config)
                    .update(LIQUIDITY_POOL)
                    .set(LIQUIDITY_POOL.AVAILABLE_BALANCE, LIQUIDITY_POOL.AVAILABLE_BALANCE.subtract(liquidityMovement.amount()).subtract(liquidityMovement.margin()))
                    .set(LIQUIDITY_POOL.LOCKED_BALANCE, LIQUIDITY_POOL.LOCKED_BALANCE.add(liquidityMovement.amount()).add(liquidityMovement.margin()))
                    .set(LIQUIDITY_POOL.UPDATED_AT, OffsetDateTime.now())
                    .where(LIQUIDITY_POOL.CURRENCY_CODE.eq(liquidityMovement.currencyCode()))
                    .returning()
                    .execute();

            var marginId = DSL.using(config)
                    .insertInto(LEDGER)
                    .set(LEDGER.CURRENCY_CODE, liquidityMovement.currencyCode())
                    .set(LEDGER.TRANSACTION_TYPE, "margin_lock")
                    .set(LEDGER.FROM_ACCOUNT, "system") // would be the currency account user
                    .set(LEDGER.TO_ACCOUNT, "system") // would be the holding account
                    .set(LEDGER.MARGIN, liquidityMovement.margin())
                    .set(LEDGER.AMOUNT, liquidityMovement.margin())
                    .set(LEDGER.TRANSACTION_ID, liquidityMovement.transactionId())
                    .set(LEDGER.DESCRIPTION, "Margin for transaction " + liquidityMovement.transactionId())
                    .returning(LEDGER.ID)
                    .fetchSingle()
                    .component1();

             return DSL.using(config)
                    .insertInto(LEDGER)
                    .set(LEDGER.CURRENCY_CODE, liquidityMovement.currencyCode())
                    .set(LEDGER.TRANSACTION_TYPE, "lock")
                    .set(LEDGER.FROM_ACCOUNT, "system") // would be the currency account user
                     //would in real life track margin seperately.  but now. it is what it is.
                     .set(LEDGER.MARGIN, liquidityMovement.margin())
                    .set(LEDGER.TO_ACCOUNT, "system") // would be the holding account
                    .set(LEDGER.AMOUNT, liquidityMovement.amount())
                    .set(LEDGER.TRANSACTION_ID, liquidityMovement.transactionId())
                    .set(LEDGER.DESCRIPTION, "Lock funds for transaction " + liquidityMovement.transactionId())
                     .returning(LEDGER.ID)
                     .fetchSingle()
                     .component1();
        }));
    }

    @Override
    public Either<Failure, Long> debitLockedBalance(Long lockId) {
        return Eithers.of(() -> dslContext.transactionResult(config -> {

            var lockedRecord = DSL.using(config)
                    .select()
                    .from(LEDGER)
                    .where(LEDGER.ID.eq(lockId))
                    .and(LEDGER.TRANSACTION_TYPE.eq("lock"))
                    .fetchSingle();

            var lockedBalance = DSL.using(config)
                    .select(
                            LIQUIDITY_POOL.LOCKED_BALANCE
                    )
                    .from(LIQUIDITY_POOL)
                    .where(LIQUIDITY_POOL.CURRENCY_CODE.eq(lockedRecord.get(LEDGER.CURRENCY_CODE)))
                    .forUpdate()
                    .fetchOptional()
                    .map(Record1::value1)
                    .orElse(BigDecimal.ZERO);

            if (lockedBalance.compareTo(lockedRecord.get(LEDGER.AMOUNT, BigDecimal.class)) < 0) {
                throw new ApplicationException(FailureCode.INSUFFICIENT_FUNDS);
            }

            DSL.using(config)
                    .update(LIQUIDITY_POOL)
                    .set(LIQUIDITY_POOL.LOCKED_BALANCE, LIQUIDITY_POOL.LOCKED_BALANCE.subtract(lockedRecord.get(LEDGER.AMOUNT, BigDecimal.class))
                            .subtract(lockedRecord.get(LEDGER.MARGIN, BigDecimal.class)))
                    .set(LIQUIDITY_POOL.UPDATED_AT, OffsetDateTime.now())
                    .where(LIQUIDITY_POOL.CURRENCY_CODE.eq(lockedRecord.get(LEDGER.CURRENCY_CODE)))
                    .returning()
                    .execute();

            DSL.using(config)
                    .insertInto(LEDGER)
                    .set(LEDGER.CURRENCY_CODE, lockedRecord.get(LEDGER.CURRENCY_CODE))
                    .set(LEDGER.TRANSACTION_TYPE, "debit")
                    .set(LEDGER.FROM_ACCOUNT, "system") // would be the currency account user
                    .set(LEDGER.TO_ACCOUNT, "system") // would be the other account
                    .set(LEDGER.AMOUNT, lockedRecord.get(LEDGER.MARGIN))
                    .set(LEDGER.TRANSACTION_ID, lockedRecord.get(LEDGER.TRANSACTION_ID))
                    .set(LEDGER.DESCRIPTION, "Margin on " + lockedRecord.get(LEDGER.TRANSACTION_ID))
                    .returning(LEDGER.ID)
                    .fetchSingle()
                    .component1();

            return DSL.using(config)
                    .insertInto(LEDGER)
                    .set(LEDGER.CURRENCY_CODE, lockedRecord.get(LEDGER.CURRENCY_CODE))
                    .set(LEDGER.TRANSACTION_TYPE, "debit")
                    .set(LEDGER.FROM_ACCOUNT, "system") // would be the currency account user
                    .set(LEDGER.TO_ACCOUNT, "system") // would be the other account
                    .set(LEDGER.AMOUNT, lockedRecord.get(LEDGER.AMOUNT))
                    .set(LEDGER.TRANSACTION_ID, lockedRecord.get(LEDGER.TRANSACTION_ID))
                    .set(LEDGER.DESCRIPTION, "Debit Position " + lockedRecord.get(LEDGER.ID))
                    .returning(LEDGER.ID)
                    .fetchSingle()
                    .component1();

        }));
    }

    @Override
    public Either<Failure, Long> unlockBalance(Long lockId) {
        return Eithers.of(() -> dslContext.transactionResult(config -> {
            var lockedRecord = DSL.using(config)
                    .select()
                    .from(LEDGER)
                    .where(LEDGER.ID.eq(lockId))
                    .and(LEDGER.TRANSACTION_TYPE.eq("lock"))
                    .fetchSingle();

            var lockedBalance = DSL.using(config)
                    .select(
                            LIQUIDITY_POOL.LOCKED_BALANCE
                    )
                    .from(LIQUIDITY_POOL)
                    .where(LIQUIDITY_POOL.CURRENCY_CODE.eq(lockedRecord.get(LEDGER.CURRENCY_CODE)))
                    .forUpdate()
                    .fetchOptional()
                    .map(Record1::value1)
                    .orElse(BigDecimal.ZERO);

            if (lockedBalance.compareTo(lockedRecord.get(LEDGER.AMOUNT, BigDecimal.class)) < 0) {
                throw new ApplicationException(FailureCode.INSUFFICIENT_FUNDS);
            }

            var record = DSL.using(config)
                    .insertInto(LEDGER)
                    .set(LEDGER.CURRENCY_CODE, lockedRecord.get(LEDGER.CURRENCY_CODE))
                    .set(LEDGER.TRANSACTION_TYPE, "unlock")
                    .set(LEDGER.FROM_ACCOUNT, "system") // would be the currency account user
                    .set(LEDGER.TO_ACCOUNT, "system") // would be the other account
                    .set(LEDGER.AMOUNT, lockedRecord.get(LEDGER.AMOUNT))
                    .set(LEDGER.TRANSACTION_ID, lockedRecord.get(LEDGER.TRANSACTION_ID))
                    .set(LEDGER.DESCRIPTION, "Unlock Position " + lockedRecord.get(LEDGER.ID))
                    .returning(LEDGER.ID)
                    .fetchSingle()
                    .component1();


            DSL.using(config)
                    .insertInto(LEDGER)
                    .set(LEDGER.CURRENCY_CODE, lockedRecord.get(LEDGER.CURRENCY_CODE))
                    .set(LEDGER.TRANSACTION_TYPE, "margin_unlock")
                    .set(LEDGER.FROM_ACCOUNT, "system") // would be the currency account user
                    .set(LEDGER.TO_ACCOUNT, "system") // would be the other account
                    .set(LEDGER.AMOUNT, lockedRecord.get(LEDGER.MARGIN))
                    .set(LEDGER.TRANSACTION_ID, lockedRecord.get(LEDGER.TRANSACTION_ID))
                    .set(LEDGER.DESCRIPTION, "Margin unlock on " + lockedRecord.get(LEDGER.TRANSACTION_ID))
                    .returning(LEDGER.ID)
                    .fetchSingle()
                    .component1();

            DSL.using(config)
                    .update(LIQUIDITY_POOL)
                    .set(LIQUIDITY_POOL.LOCKED_BALANCE, LIQUIDITY_POOL.LOCKED_BALANCE.subtract(lockedRecord.get(LEDGER.AMOUNT, BigDecimal.class))
                            .subtract(lockedRecord.get(LEDGER.MARGIN, BigDecimal.class)))
                    .set(LIQUIDITY_POOL.LOCKED_BALANCE, LIQUIDITY_POOL.AVAILABLE_BALANCE.add(lockedRecord.get(LEDGER.AMOUNT, BigDecimal.class))
                            .add(lockedRecord.get(LEDGER.MARGIN, BigDecimal.class)))
                    .set(LIQUIDITY_POOL.UPDATED_AT, OffsetDateTime.now())
                    .where(LIQUIDITY_POOL.CURRENCY_CODE.eq(lockedRecord.get(LEDGER.CURRENCY_CODE)))
                    .returning()
                    .execute();


          return record;
        }));
    }
}
