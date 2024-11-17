package com.spherelabs.repository;

import com.assetiq.jooq.tables.pojos.ExchangeRate;
import com.spherelabs.error.Failure;
import io.vavr.Tuple1;
import io.vavr.control.Either;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface ExchangeRateRepository {
    /**
     * Get latest exchange rate for a given pair
     *
     * @param currencyPair  pair of currencies to exchange eg USD/EUR
     * @return Either a Failure in case of an error or the exchange rate
     */
    Either<Failure, ExchangeRate> getLatestRate(String currencyPair);

    /**
     * Add a new exchange rate
     *
     * @param currencyPair  pair of currencies to exchange eg USD/EUR
     * @param rate          exchange rate
     * @param timestamp     timestamp of the rate
     * @return Either a Failure in case of an error or the exchange rate added
     */
    Either<Failure, ExchangeRate> addRate(String currencyPair, BigDecimal rate, OffsetDateTime timestamp);
}
