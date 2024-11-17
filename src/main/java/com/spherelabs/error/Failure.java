package com.spherelabs.error;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Failure(String message, String code, @JsonIgnore Throwable cause) {
    public static Failure of(String message, String code) {
        return new Failure(message, code, null);
    }

    public static Failure of(String message, String code, Throwable cause) {
        return new Failure(message, code, cause);
    }

    public static Failure from(FailureCode failureCode) {
        return new Failure(failureCode.getMessage(), failureCode.getCode(), null);
    }
}
