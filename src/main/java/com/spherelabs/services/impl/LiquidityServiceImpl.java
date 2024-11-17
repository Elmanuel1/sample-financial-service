package com.spherelabs.services.impl;

import com.spherelabs.error.Failure;
import com.spherelabs.model.LiquidityMovement;
import com.spherelabs.repository.LiquidityRepository;
import com.spherelabs.services.LiquidityService;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Repository
public class LiquidityServiceImpl implements LiquidityService {
    private final LiquidityRepository liquidityRepository;

    @Override
    public Either<Failure, BigDecimal> getBalance(String currency) {
        return liquidityRepository.getBalance(currency);
    }

    @Override
    public Either<Failure, Long> lockBalance(LiquidityMovement liquidityMovement) {
        return liquidityRepository.lockBalance(liquidityMovement);
    }

    @Override
    public Either<Failure, Long> debitLockedBalance(long lockId) {
        return liquidityRepository.debitLockedBalance(lockId);
    }

    @Override
    public Either<Failure, Long> unlockBalance(Long lockedId) {
        return liquidityRepository.unlockBalance(lockedId);
    }
}
