package com.spherelabs.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FailureCode {
    NOT_FOUND("record_not_found", "Record could not be found", HttpStatus.NOT_FOUND, true),
    DUPLICATE("duplicate_record", "Duplicate record detected", HttpStatus.CONFLICT, true),
    UNKNOWN_ERROR("unknown_error", "Unexpected error occurred. Please try again later", HttpStatus.INTERNAL_SERVER_ERROR, true),
    UNSUPPORTED_CURRENCY_PAIR("unsupported_currency_pair", "Unsupported currency pair", HttpStatus.UNPROCESSABLE_ENTITY, false),
    INVALID_SENDER_ACCOUNT("invalid_sender_account", "Account does not exist", HttpStatus.NOT_FOUND, false),
    INVALID_RECEIVER_ACCOUNT("invalid_receiver_account", "Account does not exist", HttpStatus.NOT_FOUND, false),
    SENDING_CURRENCY_NOT_SUPPORTED("sending_currency_not_supported", "Currency not supported", HttpStatus.UNPROCESSABLE_ENTITY, false),
    RECEIVING_CURRENCY_NOT_SUPPORTED("invalid_receiving_currency", "Currency not supported", HttpStatus.UNPROCESSABLE_ENTITY, false),
    OLD_FX_RATE("old_fx_rate_error", "Old fx rate error", HttpStatus.UNPROCESSABLE_ENTITY, false),
    NO_AVAILABLE_RATE("no_available_rate", "No available rate. We keep updating new rates. Please check back later", HttpStatus.INTERNAL_SERVER_ERROR, false),
    INSUFFICIENT_FUNDS("insufficient_funds", "Insufficient funds", HttpStatus.INTERNAL_SERVER_ERROR, false);

    private final String code;
    private final String message;
    private final HttpStatus status;
    private final boolean retryable;

    FailureCode(String code, String message, HttpStatus status, boolean retryable) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.retryable = retryable;
    }

    public static FailureCode fromCode(String code) {
        for (FailureCode failureCode : values()) {
            if (failureCode.code.equals(code)) {
                return failureCode;
            }
        }
        return FailureCode.UNKNOWN_ERROR;
    }
}
