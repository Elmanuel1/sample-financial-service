package com.spherelabs.services.impl;

import com.spherelabs.config.AppConfiguration;
import com.spherelabs.error.Failure;
import com.spherelabs.error.FailureCode;
import com.spherelabs.model.Currency;
import com.spherelabs.model.ExchangeRate;
import com.spherelabs.model.LiquidityMovement;
import com.spherelabs.model.Transaction;
import com.spherelabs.model.api.TransferRequest;
import com.spherelabs.providers.TransferProvider;
import com.spherelabs.repository.TransactionRepository;
import com.spherelabs.services.CurrencyService;
import com.spherelabs.services.ExchangeRateService;
import com.spherelabs.services.LiquidityService;
import com.spherelabs.services.TransactionService;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.spherelabs.error.FailureCode.*;


@RequiredArgsConstructor
@Slf4j
@Service
public class TransactionServiceImpl implements TransactionService {
    private final CurrencyService currencyService;
    private final ExchangeRateService exchangeRateService;
    private final AppConfiguration appConfiguration;
    private final TransactionRepository transactionRepository;
    private final LiquidityService liquidityService;
    private final TransferProvider transferProvider;
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);


    @Override
    public Either<Failure, Transaction> transfer(TransferRequest request) {
        log.info("Processing transfer request {}" , request);
        var transaction = Transaction.newTransaction(request);

        // another better option is to insert where transaction id is not equal, saves double query
        var previousRecord = transactionRepository.getTransaction(transaction.getInternalTransferId());
        // checks if an error occurred while fetching the transaction. Could be not found error
        if (previousRecord.isLeft()) {
            if (previousRecord.getLeft().code().equals(NOT_FOUND.getCode())) {
                log.debug("Transaction {} not found. Proceeding to process transaction", transaction.getInternalTransferId());
                return processTransaction(transaction);
            }
            log.error("Failed to fetch transaction {}. Reason: {}", transaction.getInternalTransferId(), previousRecord.getLeft().message(), previousRecord.getLeft().cause());
            return Either.left(Failure.from(UNKNOWN_ERROR));
        }

        log.info("Transaction {} exist", transaction.getInternalTransferId());
        return previousRecord.map(this::markAsProcessingIfApplicable);
    }

    private Either<Failure, Transaction> processTransaction(Transaction transaction) {
        var currencies = currencyService.getSupportedCurrencies();
        if (currencies.isLeft()) {
            // we could not get the supported currencies. No need to continue to process.
            // we still want to keep a copy of the transaction in the database
            log.error("Failed to get supported currencies. Reason: {}", currencies.getLeft().message());
            transaction.setFailureReason("Could not retrieve currency.  %s".formatted(currencies.getLeft().message()));
            transaction.setStatus(Transaction.Status.RETRY);
            transactionRepository.recordFailedEvent(transaction)
                    .peekLeft(failure -> log.error("Failed to insert failed transaction {}. Reason: {}", transaction.getInternalTransferId(), failure.message(), failure.cause()));
            return Either.left(currencies.getLeft());
        }

        // validate every other information
        var validation =  validate(transaction, currencies.get());

        // in case the validation fails, we still want to keep a copy of the transaction in the database
        if (validation.isLeft()) {
            transaction.setFailureReason("Validation failed. %s".formatted(validation.getLeft().message()));
            transaction.setStatus(Transaction.Status.RETRY);
            transactionRepository.recordFailedEvent(transaction)
                    .peekLeft(failure -> log.error("Failed to insert failed transaction {}. Reason: {}", transaction.getInternalTransferId(), failure.message(), failure.cause()));
            return Either.left(validation.getLeft());
        }

        log.debug("Transaction {} passed validation", transaction.getInternalTransferId());

        // modify the transaction with the exchange rate, fees and settlement info
        var recordedMessage =  modifyTransaction(currencies.get(), transaction)
            // insert the transaction. This sets the status as INITIATED
            .flatMap(__ -> transactionRepository.insertNew(transaction))
            .peek(transaction1 -> transaction.setId(transaction1.getId()))
            // lock the funds
            .flatMap(transaction1 -> liquidityService.lockBalance(LiquidityMovement.lockFrom(transaction1)))
            .flatMap(id -> transactionRepository.updateLockStatus(transaction.getId(), id, Transaction.Status.FUNDS_LOCKED, Transaction.Status.INITIATED))
            .peekLeft(failure -> log.error("Failed to process transaction. Reason: {}", failure.message(), failure.cause()));

        if (recordedMessage.isLeft()) {
            return handleSaveError(transaction, recordedMessage.getLeft())
                    . map(this::markAsProcessingIfApplicable);
        }

        var savedTransaction = recordedMessage.get();

        // ideally, we will use an async medium to process, but this is a test, so we would leave like this
        Either<Failure, Transaction.Status> transferResponse =  transferProvider.transfer(transaction)
                .peekLeft(failure -> log.error("Failed to initiate transfer. Reason: {}", failure.message(), failure.cause()));
        if (transferResponse.isLeft()) {
            savedTransaction.setStatus(Transaction.Status.PROCESSING);
            savedTransaction.setFailureReason("Failed to initiate transfer. %s".formatted(transferResponse.getLeft().code()));
        } else {
            savedTransaction.setStatus(transferResponse.get());
        }

        return transactionRepository.updateStatus(transaction.getId(), savedTransaction.getFailureReason(), savedTransaction.getStatus(), Transaction.Status.FUNDS_LOCKED)
            .peek(transaction1 -> log.info("Transaction {} initiated successfully", transaction1.getInternalTransferId()))
            .peekLeft(failure -> log.error("Failed to process transaction. Reason: {}", failure.message(), failure.cause()));

    }

    private Transaction markAsProcessingIfApplicable(Transaction transaction1) {
        if (transaction1.getStatus().ordinal() <= Transaction.Status.PROCESSING.ordinal()) {
            transaction1.setStatus(Transaction.Status.PROCESSING);
        }

        return transaction1;
    }

    //just basic validations
    private Either<Failure, Void> validate(Transaction transaction, List<Currency> currencies) {
        if (transaction.getSenderAccount().endsWith("111")) {
            return Either.left(Failure.from(INVALID_SENDER_ACCOUNT));
        }

        if (transaction.getReceiverAccount().endsWith("111")) {
            return Either.left(Failure.from(INVALID_RECEIVER_ACCOUNT));
        }

        var fromCurrency = currencies.stream()
                .filter(currency -> currency.code().equals(transaction.getFromCurrency()))
                .findFirst();
        if (fromCurrency.isEmpty()) {
            return Either.left(Failure.from(SENDING_CURRENCY_NOT_SUPPORTED));
        }

        var toCurrency = currencies.stream()
                .filter(currency -> currency.code().equals(transaction.getToCurrency()))
                .findFirst();
        if (toCurrency.isEmpty()) {
            return Either.left(Failure.from(RECEIVING_CURRENCY_NOT_SUPPORTED));
        }

        if (!appConfiguration.getMarginRates().containsKey(transaction.getToCurrency())) {
            return Either.left(Failure.from(UNSUPPORTED_CURRENCY_PAIR));
        }

        return Either.right(null);
    }


    private Either<Failure, Transaction> handleSaveError(Transaction transaction, Failure failure) {
        if (failure.code().equals(INSUFFICIENT_FUNDS.getCode())) {
            transaction.setStatus(Transaction.Status.FAILED);
            transaction.setFailureReason("Insufficient funds");
            return transactionRepository.markSettlementStatus(transaction.getId(), failure.code() + failure.message(), Transaction.SettlementStatus.SETTLEMENT_STOPPED, Transaction.Status.FAILED, Transaction.Status.INITIATED)
                    .peekLeft(failure1 -> log.error("Failed to update transaction {}. Reason: {}", transaction.getInternalTransferId(), failure1.message(), failure1.cause()));
        }

        SCHEDULED_EXECUTOR_SERVICE.scheduleWithFixedDelay(() -> determineFinalStatus(transaction.getInternalTransferId()), 0, 3, TimeUnit.MINUTES);
        return Either.left(failure);
    }

    private void determineFinalStatus(String transferId) {
        // check if the transaction is still in the INITIATED state
    }

    private Either<Failure, Void> modifyTransaction(List<Currency> currencies,  Transaction transaction) {
        return exchangeRateService.getLatestRate("%s/%s".formatted(transaction.getFromCurrency(), transaction.getToCurrency()))
                .mapLeft(failure -> mapNotFoundToSpecificFailure(failure, NO_AVAILABLE_RATE))
                .peek(exchangeRate -> modifyTransactionFees(transaction, appConfiguration.getMarginRates().get(transaction.getToCurrency()), currencies, exchangeRate))
                .peek(exchangeRate -> modifySettlementInfo(transaction, currencies))
                .map(__ -> null);
    }

    private Failure mapNotFoundToSpecificFailure(Failure failure, FailureCode failureCode) {
        if (failure.code().equals(NOT_FOUND.getCode())) {
            return Failure.from(failureCode);
        }
        return failure;
    }

    private void modifySettlementInfo(Transaction transaction, List<Currency> currencies) {
        // i expect to see a fee here since we validated the currency pair against the fees map already
        currencies.stream()
                .filter(currency -> currency.code().equals(transaction.getFromCurrency()))
                .findFirst()
                .ifPresentOrElse(currency -> {
                            transaction.setSettlementWindow(currency.settlementTime());
                            transaction.setScheduledSettlementTime(transaction.getCreatedAt().plus(currency.settlementTime()));
                        },
                        () -> log.warn("CUnexpected currency {} not found in the list of supported currencies", transaction.getFromCurrency()));
    }

    private void modifyTransactionFees(Transaction transaction, BigDecimal marginRate, List<Currency> currencies, ExchangeRate exchangeRate) {
        int precision = currencies.stream()
                .filter(currency -> currency.code().equals(transaction.getToCurrency()))
                .findFirst()
                .map(Currency::precision)
                .orElse(2);


        BigDecimal exchangeAmount = transaction.getFromAmount()
                .multiply(exchangeRate.rate())
                .setScale(precision, RoundingMode.HALF_UP);


        BigDecimal margin = exchangeAmount
                .multiply(marginRate)
                .setScale(precision, RoundingMode.HALF_UP);

        transaction.setToAmount(exchangeAmount.subtract(margin));

        transaction.setMargin(margin);
        transaction.setMarginCurrency(transaction.getToCurrency());
        transaction.setMarginRate(marginRate);

        transaction.setFxRate(exchangeRate.rate());
        transaction.setEffectiveRateDate(exchangeRate.timestamp());

    }
}
