package com.spherelabs.model;

import java.time.Duration;
import java.time.OffsetDateTime;

public record Currency(String code, Duration settlementTime, OffsetDateTime createdAt, OffsetDateTime updatedAt, boolean enabled, int precision) {
    public static Currency from(com.assetiq.jooq.tables.pojos.Currency currency) {
        return new Currency(
                currency.getCode(),
                currency.getSettlementTime().toDuration(),
                currency.getCreatedAt(),
                currency.getUpdatedAt(),
                currency.getEnabled(),
                currency.getPrecision()
        );
    }
}
