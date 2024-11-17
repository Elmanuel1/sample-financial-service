package com.spherelabs.repository.impl;

import com.assetiq.jooq.tables.pojos.ExchangeRate;
import com.spherelabs.error.Failure;
import com.spherelabs.repository.ExchangeRateRepository;
import com.spherelabs.utils.Eithers;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static com.assetiq.jooq.Tables.EXCHANGE_RATE;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ExchangeRateRepositoryImpl implements ExchangeRateRepository {
    private final DSLContext ctx;
    @Override
    public Either<Failure, ExchangeRate> getLatestRate(String currencyPair) {
         return Eithers.of(() -> ctx.select()
                .from(EXCHANGE_RATE)
                .where(EXCHANGE_RATE.CURRENCY_PAIR.eq(currencyPair))
                 .and(EXCHANGE_RATE.EFFECTIVE_DATE.le(OffsetDateTime.now()))
                .orderBy(EXCHANGE_RATE.EFFECTIVE_DATE.desc())
                .limit(1)
                .fetchOptionalInto(ExchangeRate.class)
                 .get()
         ).peekLeft(failure -> log.error("Failed to get latest rate for currency pair: {}. Reason: {}", currencyPair, failure.message(), failure.cause()));

    }

    @Override
    public Either<Failure, ExchangeRate> addRate(String currencyPair, BigDecimal rate, OffsetDateTime timestamp) {
        return Eithers.of(() -> ctx.insertInto(EXCHANGE_RATE)
                .set(EXCHANGE_RATE.CURRENCY_PAIR, currencyPair)
                .set(EXCHANGE_RATE.RATE, rate)
                .set(EXCHANGE_RATE.EFFECTIVE_DATE, timestamp)
                .returning()
                .fetchOne()
                .into(ExchangeRate.class));
    }
}
