package com.spherelabs.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Transfer(
    String senderAccount,
    String receiverAccount,
    String fromCurrency,
    String toCurrency,
    BigDecimal amount,
    BigDecimal fxRate,
    BigDecimal fee,
    BigDecimal marginPercentage,
    String status,
    OffsetDateTime settlementTime,
    OffsetDateTime dateSettled
) {
    /**
     * This keeps it simple since the task does not mention sent
     */
    public enum Status {
        /**
         * Initial status for the transaction.  Account is just settled.
         */
        PENDING,
        PROCESSING,
        FAILED,
        SENT,
    }
}
