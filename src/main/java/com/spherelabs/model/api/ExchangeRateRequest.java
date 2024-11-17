package com.spherelabs.model.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.spherelabs.annotations.CurrencyPair;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExchangeRateRequest(@CurrencyPair @NotBlank String pair,
                                  @NotNull @Positive BigDecimal rate,
                                  @NotNull  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX") OffsetDateTime timestamp) { }
