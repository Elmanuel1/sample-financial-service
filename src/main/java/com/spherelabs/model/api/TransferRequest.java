package com.spherelabs.model.api;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record TransferRequest(
    /**
     * Account number of the sender
     */
    @NotBlank String senderAccount,

    /**
     * Account number of the receiver
     */
    @NotBlank String receiverAccount,

    /**
     * Currency of the sender
     */
    @NotBlank @Size(max = 3) String fromCurrency,

    /**
     * Currency of the receiver
     */
    @NotBlank @Size(max = 3) String toCurrency,

    /**
     * Amount to transfer
     */
    @NotNull @Positive BigDecimal amount,

    /**
     * Description of the transaction
     */
    @NotBlank @Size(max = 50) String description,

    /**
     * The reference of the transaction
     */
    @NotBlank @Size(max = 30) String reference
) {}
