package com.spherelabs.utils;

import com.spherelabs.error.ApplicationException;
import com.spherelabs.error.Failure;
import com.spherelabs.error.FailureCode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.NoDataFoundException;
import org.springframework.dao.DuplicateKeyException;

import java.util.NoSuchElementException;


@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorUtil {

    public static Failure handleError(Throwable cause) {
        var failureCode =  switch (cause) {
            case NoDataFoundException __ -> FailureCode.NOT_FOUND;
            case DataAccessException e -> dataAccessErrorReason(e);
            case DuplicateKeyException __ -> FailureCode.DUPLICATE;
            case NoSuchElementException __ -> FailureCode.NOT_FOUND;
            case ApplicationException e ->  FailureCode.fromCode(e.getError().code());
            default -> FailureCode.UNKNOWN_ERROR;
        };

        return Failure.of( failureCode.getMessage(), failureCode.getCode(), cause);
    }

    private static FailureCode dataAccessErrorReason(DataAccessException exception) {
      return switch (exception.sqlState()) {
        case "23505" ->  FailureCode.DUPLICATE;
        default -> FailureCode.UNKNOWN_ERROR;
      };
    }
}