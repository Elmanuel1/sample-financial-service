package com.spherelabs.processors;

import com.assetiq.jooq.enums.TransactionStatus;
import com.spherelabs.error.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static com.assetiq.jooq.tables.Ledger.LEDGER;
import static com.assetiq.jooq.tables.LiquidityPool.LIQUIDITY_POOL;
import static com.assetiq.jooq.tables.Transaction.TRANSACTION;
import static org.jooq.impl.DSL.*;
@Service
@Slf4j
@RequiredArgsConstructor
public class PoolRebalanceProcessor {
    private final DSLContext dsl;

    @Value("${app.rebalance-look-back-minutes:30}")
    private int lookBackMinutes;

    @Value("${app.rebalance-threshold-ratio:0.2}")
    private BigDecimal thresholdRatio;

    @Value("${app.rebalance-minimum-amount:1000}")
    private BigDecimal minimumRebalanceAmount;

    @Value("${app.rebalance-safety-buffer-ratio:0.3}")
    private BigDecimal safetyBufferRatio;

    @Scheduled(fixedRateString = "${app.rebalance-check-minutes:5}")
    public void analyzeAndRebalance() {
        try {
            log.info("Starting pool rebalance analysis");

            Map<String, FlowMetrics> flows = getFlowMetrics();
            if (flows.isEmpty()) {
                log.info("No flows found for analysis");
                return;
            }

            Map<String, PoolBalance> poolBalances = getCurrentPoolBalances();
            if (poolBalances.isEmpty()) {
                log.warn("No pool balances found");
                return;
            }

            flows.forEach((currency, metrics) -> {
                try {
                    processRebalancing(currency, metrics, poolBalances);
                } catch (Exception e) {
                    log.error("Error processing rebalance for currency {}: {}", currency, e.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("Error during pool rebalancing: {}", e.getMessage());
        }
    }

    private Map<String, FlowMetrics> getFlowMetrics() {
        return dsl
                .select(
                        TRANSACTION.TO_CURRENCY,
                        count().as("transfer_count"),
                        sum(TRANSACTION.TO_AMOUNT).as("total_amount"),
                        sum(TRANSACTION.MARGIN).as("total_margin")
                )
                .from(TRANSACTION)
                .where(TRANSACTION.CREATED_AT.gt(OffsetDateTime.now().minusMinutes(lookBackMinutes)))
                .and(TRANSACTION.STATUS.eq(TransactionStatus.completed))
                .groupBy(TRANSACTION.TO_CURRENCY)
                .fetchMap(
                        r -> r.get(TRANSACTION.TO_CURRENCY),
                        r -> new FlowMetrics(
                                r.get("transfer_count", Integer.class),
                                r.get("total_amount", BigDecimal.class),
                                r.get("total_margin", BigDecimal.class)
                        )
                );
    }

    private Map<String, PoolBalance> getCurrentPoolBalances() {
        return dsl
                .select(
                        LIQUIDITY_POOL.CURRENCY_CODE,
                        LIQUIDITY_POOL.AVAILABLE_BALANCE,
                        LIQUIDITY_POOL.LOCKED_BALANCE
                )
                .from(LIQUIDITY_POOL)
                .fetchMap(
                        r -> r.get(LIQUIDITY_POOL.CURRENCY_CODE),
                        r -> new PoolBalance(
                                r.get(LIQUIDITY_POOL.AVAILABLE_BALANCE),
                                r.get(LIQUIDITY_POOL.LOCKED_BALANCE)
                        )
                );
    }

    private void processRebalancing(
            String currency,
            FlowMetrics metrics,
            Map<String, PoolBalance> poolBalances
    ) {
        PoolBalance pool = poolBalances.get(currency);
        if (pool == null) {
            log.warn("Missing pool balance for currency {}", currency);
            return;
        }

        if (needsRebalancing(pool, metrics, currency)) {
            BigDecimal amount = calculateRebalanceAmount(pool, metrics);
            if (amount.compareTo(minimumRebalanceAmount) >= 0) {
                //in the case the amount to rebalance by is above the minimum threshold
                executeRebalancing(currency, amount);
            } else {
                log.info("Rebalance amount {} for currency {} is below minimum threshold {}", amount, currency, minimumRebalanceAmount);
            }
        }
    }


    private boolean needsRebalancing(PoolBalance pool, FlowMetrics metrics, String currency) {
        BigDecimal totalFlow = metrics.totalAmount().add(metrics.totalMargin());
        BigDecimal availableRatio = pool.availableBalance()
                .divide(totalFlow, RoundingMode.HALF_UP);

        log.info("Available balance ratio for currency {}: is {}. Threshold ratio is {}", currency,  availableRatio, thresholdRatio);
        return availableRatio.compareTo(thresholdRatio) < 0;
    }

    private BigDecimal calculateRebalanceAmount(
            PoolBalance pool,
            FlowMetrics metrics
    ) {
        BigDecimal totalFlow = metrics.totalAmount().add(metrics.totalMargin());
        BigDecimal targetBalance = totalFlow.multiply(BigDecimal.ONE.add(safetyBufferRatio));
        BigDecimal currentAvailable = pool.availableBalance();

        return targetBalance.subtract(currentAvailable);
    }

    protected void executeRebalancing(String currency, BigDecimal amount) {
        String rebalanceId = "rebalance_" + UUID.randomUUID();

        try {
            dsl.transaction(config -> {
                DSLContext ctx = DSL.using(config);

                // Record rebalancing entry
                ctx.insertInto(LEDGER)
                        .set(LEDGER.CURRENCY_CODE, currency)
                        .set(LEDGER.FROM_ACCOUNT, "master_pool")
                        .set(LEDGER.TO_ACCOUNT, currency + "_pool")
                        .set(LEDGER.TRANSACTION_TYPE, "rebalance")
                        .set(LEDGER.AMOUNT, amount)
                        .set(LEDGER.TRANSACTION_ID, rebalanceId)
                        .set(LEDGER.DESCRIPTION,
                                String.format("Pool rebalancing for %s", currency))
                        .execute();

                // Update pool balance
                ctx.update(LIQUIDITY_POOL)
                        .set(LIQUIDITY_POOL.AVAILABLE_BALANCE,
                                LIQUIDITY_POOL.AVAILABLE_BALANCE.add(amount))
                        .where(LIQUIDITY_POOL.CURRENCY_CODE.eq(currency))
                        .execute();

                log.info("Rebalanced pool {} by adding {}", currency, amount);
            });
        } catch (Exception e) {
            log.error("Failed to execute rebalancing {}: {}", rebalanceId, e.getMessage());
            throw new ApplicationException("Rebalancing failed", e);
        }
    }

    record FlowMetrics(
            int transferCount,
            BigDecimal totalAmount,    // TO_AMOUNT
            BigDecimal totalMargin     // MARGIN
    ){}

    record PoolBalance(
            BigDecimal availableBalance,
            BigDecimal lockedBalance
    ){}
}