package com.siddnotani.expensetracker.bff;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BffExceptionHandler {
    @ExceptionHandler(BffDownstreamException.class)
    ProblemDetail downstream(BffDownstreamException exception) {
        HttpStatus status = exception.status() != null && exception.status().is4xxClientError()
                ? HttpStatus.valueOf(exception.status().value())
                : HttpStatus.BAD_GATEWAY;
        return ProblemDetail.forStatusAndDetail(status, "A downstream service failed");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
