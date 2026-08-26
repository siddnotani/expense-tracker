package com.siddnotani.expensetracker.bff.api;

import com.siddnotani.expensetracker.shared.security.SharedWebConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BffApiExceptionHandling {
    private static final Logger LOGGER = LoggerFactory.getLogger(BffApiExceptionHandling.class);

    @ExceptionHandler(DownstreamException.class)
    ResponseEntity<ProblemDetail> downstream(DownstreamException exception) {
        if (exception.getStatus().is5xxServerError()) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_GATEWAY, "Downstream service unavailable");
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem);
        }
        return ResponseEntity.status(exception.getStatus()).body(exception.getProblem());
    }

    @ExceptionHandler(BalanceSyncException.class)
    ResponseEntity<ProblemDetail> balanceSync(BalanceSyncException exception) {
        String requestId = MDC.get(SharedWebConfiguration.REQUEST_ID);
        LOGGER.error(
                "Transaction was recorded but account balance synchronization failed"
                        + " requestId={} transactionId={} accountId={}",
                requestId, exception.getTransactionId(), exception.getAccountId(), exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "The transaction was recorded but the account balance is stale");
        problem.setProperty("transactionId", exception.getTransactionId());
        problem.setProperty("accountId", exception.getAccountId());
        problem.setProperty("requestId", requestId);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem);
    }
}
