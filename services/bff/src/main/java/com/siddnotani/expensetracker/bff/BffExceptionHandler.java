package com.siddnotani.expensetracker.bff;

import com.siddnotani.expensetracker.bff.api.BalanceSyncException;
import com.siddnotani.expensetracker.bff.api.DownstreamException;
import com.siddnotani.expensetracker.shared.security.SharedWebConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class BffExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BffExceptionHandler.class);

    @ExceptionHandler(BffDownstreamException.class)
    ProblemDetail downstream(BffDownstreamException exception) {
        if (exception.status() == null || exception.status().is5xxServerError()) {
            return ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_GATEWAY, exception.problem().getDetail());
        }
        return exception.problem();
    }

    @ExceptionHandler(DownstreamException.class)
    ProblemDetail downstream(DownstreamException exception) {
        if (exception.getStatus().is5xxServerError()) {
            return ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_GATEWAY, exception.getProblem().getDetail());
        }
        return exception.getProblem();
    }

    @ExceptionHandler(BalanceSyncException.class)
    ProblemDetail balanceSync(BalanceSyncException exception) {
        String requestId = MDC.get(SharedWebConfiguration.REQUEST_ID);
        LOGGER.error(
                "Transaction was recorded but account balance synchronization failed requestId={}"
                        + " transactionId={} accountId={}",
                requestId,
                exception.getTransactionId(),
                exception.getAccountId(),
                exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "The transaction was recorded but the account balance is stale");
        problem.setProperty("transactionId", exception.getTransactionId());
        problem.setProperty("accountId", exception.getAccountId());
        problem.setProperty("requestId", requestId);
        return problem;
    }

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail responseStatus(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(exception.getStatusCode().value()), exception.getReason());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
