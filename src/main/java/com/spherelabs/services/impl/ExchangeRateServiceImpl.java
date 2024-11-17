package com.spherelabs.services.impl;

import com.spherelabs.error.Failure;
import com.spherelabs.error.FailureCode;
import com.spherelabs.model.ExchangeRate;
import com.spherelabs.repository.ExchangeRateRepository;
import com.spherelabs.services.CurrencyService;
import com.spherelabs.services.ExchangeRateService;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static com.spherelabs.error.FailureCode.OLD_FX_RATE;
import static com.spherelabs.error.FailureCode.UNSUPPORTED_CURRENCY_PAIR;

@RequiredArgsConstructor
@Service
@Slf4j
public class ExchangeRateServiceImpl implements ExchangeRateService {
    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyService currencyService;

    /**
     * This method caches the successful result to avoid constant database reads (ideal case)
     */
    @Override
    @Cacheable(value = "rateCache", key = "#currencyPair",  condition = "#result.isRight()")
    public Either<Failure, ExchangeRate> getLatestRate(String currencyPair) {
        return exchangeRateRepository.getLatestRate(currencyPair)
                .map(exchangeRate -> new ExchangeRate(exchangeRate.getCurrencyPair(), exchangeRate.getRate(), exchangeRate.getEffectiveDate()));
    }

    /**
     * This method caches the successful result to avoid constant database writes
     */
    @Override
    @CachePut(value = "rateCache", key = "#currencyPair",  condition = "#result.isRight()")
    public Either<Failure, ExchangeRate> addRate(String currencyPair, BigDecimal rate, OffsetDateTime timestamp) {
        //this value has been validated at input in the controller
        String[] pair = currencyPair.split("/");
        log.debug("Adding rate for currency pair: {}", currencyPair);
        return currencyService.getSupportedCurrencies()
                .filterOrElse(currencies -> currencies.stream().anyMatch(currency -> currency.code().equals(pair[0]) || currency.code().equals(pair[1])),
                        ignored -> Failure.from(UNSUPPORTED_CURRENCY_PAIR))
                .flatMap(__ -> validateTimestamp(currencyPair, timestamp) )
                .filterOrElse(valid -> valid, __ -> Failure.from(OLD_FX_RATE))
                .flatMap(__ -> exchangeRateRepository.addRate(currencyPair, rate, timestamp))
                .map(exchangeRate -> new ExchangeRate(exchangeRate.getCurrencyPair(), exchangeRate.getRate(), exchangeRate.getEffectiveDate()))
                .peekLeft(failure -> log.error("Failed to add rate for currency pair: {}. Reason: {}", currencyPair, failure.message()))
                .peek(exchangeRate -> log.debug("Added rate for currency pair: {}. Rate {}", currencyPair, exchangeRate));
    }

    private Either<Failure, Boolean> validateTimestamp(String currencyPair, OffsetDateTime timestamp) {
        var latestRate = this.getLatestRate(currencyPair);
        if (latestRate.isLeft()) {
            if (FailureCode.NOT_FOUND.getCode().equals(latestRate.getLeft().code())) {
                log.debug("No rate found for currency pair: {}. Safe to insert", currencyPair);
                return Either.right(true);
            }
            return Either.left(latestRate.getLeft());
        }

        // returns true if timestamp sent in api is after the last timestamp in the database
        return Either.right(latestRate.get().timestamp().isBefore(timestamp));
    }
}
