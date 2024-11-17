package com.spherelabs.repository.impl;

import com.assetiq.jooq.tables.pojos.Currency;
import com.spherelabs.error.Failure;
import com.spherelabs.repository.CurrencyRepository;
import com.spherelabs.utils.Eithers;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.assetiq.jooq.Tables.CURRENCY;

@RequiredArgsConstructor
@Repository
public class CurrencyRepositoryImpl implements CurrencyRepository {
    private final DSLContext ctx;
    @Override
    public Either<Failure, List<Currency>> getAllCurrencies() {
        return Eithers.of(() -> ctx.select()
                .from(CURRENCY)
                .fetchInto(Currency.class));
    }
}
