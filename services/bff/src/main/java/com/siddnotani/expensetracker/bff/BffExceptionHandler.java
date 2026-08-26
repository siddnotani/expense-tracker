package com.siddnotani.expensetracker.bff;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
class BffExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail responseStatus(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(exception.getStatusCode().value()), exception.getReason());
    }
}
