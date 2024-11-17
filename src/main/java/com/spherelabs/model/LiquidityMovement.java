package com.spherelabs.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LiquidityMovement(
        Long id,
        String transactionId,
        String currencyCode,
        Transaction.Type transactionType,
        BigDecimal amount,
        OffsetDateTime eventTime,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String description,
        BigDecimal margin
) {
    public LiquidityMovement(String transactionId, String currencyCode, Transaction.Type transactionType, BigDecimal amount, OffsetDateTime eventTime, String description, BigDecimal margin) {
        this(null, transactionId, currencyCode, transactionType, amount, eventTime, null, null, description, margin);
    }

    public static LiquidityMovement lockFrom(Transaction transaction) {
        return new LiquidityMovement(
                transaction.getInternalTransferId(),
                transaction.getToCurrency(),
                Transaction.Type.LOCK,
                transaction.getToAmount(),
                transaction.getCreatedAt(),
                transaction.getDescription(),
                transaction.getMargin()
        );
    }
}
