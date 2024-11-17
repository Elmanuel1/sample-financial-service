package com.spherelabs.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExchangeRate(String currencyPair, BigDecimal rate, OffsetDateTime timestamp) { }
