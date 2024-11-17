package com.spherelabs.error;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

public class ErrorTranslator {

    public static APIError from(BindingResult errors) {
        return new APIError("api.validation.error", getValidationMessage(errors.getFieldError()));
    }

    private static String getValidationMessage(ObjectError error) {
        if (error instanceof FieldError fieldError) {
            String property = fieldError.getField();
            Object invalidValue = fieldError.getRejectedValue();
            String message = fieldError.getDefaultMessage();
            return "%s: %s. Rejected value:  %s".formatted(property, message, invalidValue);
        }
        return "Your request is invalid. Please check your request and try again";
    }

    public static APIError from(HttpMessageNotReadableException __) {
        return new APIError(
                "api.validation.error", "Could not parse your request. Please modify your request and try again");
    }
}
