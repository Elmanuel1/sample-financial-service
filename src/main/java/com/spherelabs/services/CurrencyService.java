package com.spherelabs.services;

import com.spherelabs.error.Failure;
import com.spherelabs.model.Currency;
import io.vavr.control.Either;

import java.util.List;

public interface CurrencyService {
    /**
     * Get all supported currencies
     *
     * @return Either a Failure in case of an error or the list of supported currencies
     */
    Either<Failure, List<Currency>> getSupportedCurrencies();

    /**
     * Get all currencies
     *
     * @return Either a Failure in case of an error or the list of currencies
     */
    Either<Failure, List<Currency>> getAllCurrencies();
}
