package com.spherelabs.services.impl;

import com.spherelabs.error.Failure;
import com.spherelabs.model.Currency;
import com.spherelabs.repository.CurrencyRepository;
import com.spherelabs.services.CurrencyService;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class CurrencyServiceImpl implements CurrencyService {
    private final CurrencyRepository repository;

    @Cacheable(value = "currencyCache", key = "'supportedCurrencies'",  condition = "#result.isRight()")
    @Override
    public Either<Failure, List<Currency>> getSupportedCurrencies() {
        return repository.getAllCurrencies()
                .map(currencies ->
                        currencies.stream()
                                .filter(com.assetiq.jooq.tables.pojos.Currency::getEnabled)
                                .map(Currency::from)
                                .toList());
    }

    @Override
    public Either<Failure, List<Currency>> getAllCurrencies() {
        return repository.getAllCurrencies()
                .map(currencies -> currencies.stream()
                        .map(Currency::from)
                        .toList());
    }
}
