package com.spherelabs.model;

import com.spherelabs.model.api.TransferRequest;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;

@Data
public class Transaction {
    private Long id;
    private String transferId;
    private String internalTransferId;
    private String senderAccount;
    private String receiverAccount;
    private BigDecimal fromAmount;
    private String fromCurrency;
    private Long lockedId;
    private Long unlockedId;
    private BigDecimal toAmount;
    private String toCurrency;
    private BigDecimal margin;
    private String marginCurrency;
    private BigDecimal fxRate;
    private BigDecimal marginRate;
    private Status status;
    private OffsetDateTime scheduledSettlementTime;
    private OffsetDateTime effectiveRateDate;
    private Duration settlementWindow;
    private Integer settlementAttempts;
    private String settlementMessage;
    private SettlementStatus settlementStatus;
    private OffsetDateTime actualSettlementTime;
    private String failureReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String description;

    public boolean isEligibleForSettlement() {
        return settlementStatus == null;
    }


    // Status enum with a constructor for string values
    @Getter
    public enum Status {
        INITIATED("initiated"),
        FUNDS_LOCKED("funds_locked"),
        PROCESSING("processing"),
        COMPLETED("completed"),
        FAILED("failed"),
        EXPIRED("expired"),
        REQUIRE_INTERVENTION("require_intervention"),
        SETTLED("settled"),


        // only to be used by the client to know it is safe to retry
        // transactions put in the failed ransactions event table is eligible
        RETRY("retry");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public static Status from(String value) {
            for (Status status : Status.values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid status value: " + value);
        }

    }

    @Getter
    public enum SettlementStatus {
        REQUIRE_INTERVENTION("require_intervention"),
        SETTLED("settled"),
        SETTLEMENT_STOPPED("settlement_stopped");

        // only to be used by the client to know it is safe to retry
        // transactions put in the failed ransactions event table is eligible

        private final String value;

        SettlementStatus(String value) {
            this.value = value;
        }

        public static SettlementStatus from(String value) {
            for (SettlementStatus status : SettlementStatus.values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid status value: " + value);
        }

    }

    // Type enum with a constructor for string values
    public enum Type {
        DEBIT("debit"),
        CREDIT("credit"),
        LOCK("lock"),
        UNLOCK("unlock"),
        FEE("fee");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public static Type from(String transactionType) {
            for (Type type : Type.values()) {
                if (type.getValue().equals(transactionType)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid transaction type value: " + transactionType);
        }

        public String getValue() {
            return value;
        }
    }

    public static Transaction newTransaction(TransferRequest transfer) {

        var transaction = new Transaction();
        transaction.setTransferId(transfer.reference());
        transaction.setInternalTransferId("system|%s|%s|%s".formatted(transfer.reference(), transfer.fromCurrency(),transfer.toCurrency()));
        transaction.setSenderAccount(transfer.senderAccount());
        transaction.setReceiverAccount(transfer.receiverAccount());
        transaction.setFromAmount(transfer.amount());
        transaction.setFromCurrency(transfer.fromCurrency());
        transaction.setToCurrency(transfer.toCurrency());
        transaction.setCreatedAt(OffsetDateTime.now());
        transaction.setStatus(Status.INITIATED);
        transaction.setDescription(transfer.description());
        return transaction;
    }


}
