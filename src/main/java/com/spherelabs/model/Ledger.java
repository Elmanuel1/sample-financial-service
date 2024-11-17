package com.spherelabs.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Ledger(
        Long id,
        String transactionId,
        String currencyCode,
        Transaction.Type transactionType,
        BigDecimal amount,
        OffsetDateTime createdAt,
        String description
) {

    @Getter
    public enum TransactionType {
        DEBIT("debit"),
        CREDIT("credit"),
        LOCK("lock"),
        UNLOCK("unlock"),
        FEE("fee");

        private final String value;

        TransactionType(String value) {
            this.value = value;
        }

    }
}
