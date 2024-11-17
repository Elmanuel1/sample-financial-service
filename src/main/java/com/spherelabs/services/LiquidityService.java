package com.spherelabs.services;

import com.spherelabs.error.Failure;
import com.spherelabs.model.LiquidityMovement;
import io.vavr.control.Either;

import java.math.BigDecimal;

public interface LiquidityService {
    /**
     * Get the live balance of the account
     * @param currency Currency
     * @return Either a Failure or the balance
     */
    Either<Failure, BigDecimal> getBalance(String currency);

    Either<Failure, Long> lockBalance(LiquidityMovement transaction1);

    /**
     * Unlock the balance
     * @param lockId The lock id
     * @return Either a Failure or the lock id
     */
    Either<Failure, Long> debitLockedBalance(long lockId);

    /**
     * Return the locked funds to available balance
     *
     * @return Either a Failure if it failed to return the funds
     */

    Either<Failure, Long> unlockBalance(Long lockedId);
}
