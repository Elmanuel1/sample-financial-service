package com.spherelabs.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApplicationException extends RuntimeException {
    private APIError error;
    private HttpStatus httpStatus;

    public ApplicationException(FailureCode failureCode) {
        super(failureCode.getMessage());
        this.error = new APIError(failureCode.getCode(), failureCode.getMessage());
        this.httpStatus = failureCode.getStatus();
    }

    public ApplicationException(Failure failure) {
        super(failure.message());
        var failureCode = FailureCode.fromCode(failure.code());
        this.error = new APIError(failureCode.getCode(), failureCode.getMessage());
        this.httpStatus = failureCode.getStatus();
    }

    public ApplicationException(String message) {
        this.error = new APIError("api.validation.error", message);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public ApplicationException(String rebalancingFailed, Exception e) {
        super(rebalancingFailed, e);
    }
}
