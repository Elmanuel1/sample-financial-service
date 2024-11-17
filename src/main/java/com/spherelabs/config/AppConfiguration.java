package com.spherelabs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Map;

@ConfigurationProperties(prefix = "app")
@Data
@Component
@Validated
public class AppConfiguration {
    private Map<String, BigDecimal> marginRates;
    private int settlementPollSize;
    private int maxSettlementAttempts;
}