package com.spherelabs.repository;

import com.spherelabs.error.Failure;
import com.spherelabs.model.LiquidityMovement;
import io.vavr.control.Either;

import java.math.BigDecimal;

public interface LiquidityRepository {
    /**
     * Get the live balance of the account.
     * in the case the account does not exist or currency, it returns 0
     * @param currency Currency
     * @return Either a Failure or the balance
     */
    Either<Failure, BigDecimal> getBalance(String currency);

    /**
     * Try to lock the balance of the account.
     *
     * @param liquidityMovement Liquidity to lock
     *                             Could return a Failure if the balance is insufficient
     * @return Either a Failure or the lock id
     */
    Either<Failure, Long> lockBalance(
           LiquidityMovement liquidityMovement
    );

    /**
     * Try to lock the balance of the account.
     *Could return a Failure if the balance is insufficient
     * @param lockId The lock id
     *
     * @return Either a Failure or the lock id
     */
    Either<Failure, Long> unlockBalance(
            Long lockId
    );

    /**
     * Unlock the balance of the account.
     *
     * @param lockId The lock id
     * @return Either a Failure or the balance history. Which is the debit entry
     */
    Either<Failure, Long> debitLockedBalance(Long lockId);
}
