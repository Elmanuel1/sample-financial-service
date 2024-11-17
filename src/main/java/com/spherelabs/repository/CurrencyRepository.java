package com.spherelabs.repository;

import com.assetiq.jooq.tables.pojos.Currency;
import com.spherelabs.error.Failure;
import io.vavr.control.Either;

import java.util.List;

public interface CurrencyRepository {
    /**
     * Get all currencies
     *
     * @return Either a Failure in case of an error or the list of currencies
     */
    Either<Failure, List<Currency>> getAllCurrencies();
}