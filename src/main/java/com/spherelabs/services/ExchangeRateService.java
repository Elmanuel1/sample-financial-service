package com.spherelabs.services;

import com.spherelabs.error.Failure;
import com.spherelabs.model.ExchangeRate;
import io.vavr.control.Either;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/**
 * Exchange rate service
 * <p>
 *     This service provides methods to get exchange rate from one currency to another
 *     and add exchange rate pair
 */
public interface ExchangeRateService {

    /**
     * Get latest exchange rate from one currency to another
     *
     * @param currencyPair  pair of currencies to exchange eg USD/EUR
     * @return Either a Failure in case of an error or the exchange rate
     */
    Either<Failure, ExchangeRate> getLatestRate(String currencyPair);

    /**
     * Add exchange rate pair
     *
     * @param currencyPair  pair of currencies to exchange eg USD/EUR
     * @param rate exchange rate
     * @param timestamp timestamp of the exchange rate
     * @return Either a Failure in case of an error or the exchange rate
     */

    Either<Failure, ExchangeRate> addRate(String currencyPair, BigDecimal rate, OffsetDateTime timestamp);
}
