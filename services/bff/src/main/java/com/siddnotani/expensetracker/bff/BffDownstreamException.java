package com.siddnotani.expensetracker.bff;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class BffDownstreamException extends RuntimeException {
    private final HttpStatusCode status;
    private final ProblemDetail problem;

    public BffDownstreamException(Throwable cause) {
        super("Downstream service unavailable", cause);
        this.status = null;
        this.problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, "Downstream service unavailable");
    }

    private BffDownstreamException(WebClientResponseException cause) {
        super("Downstream service returned an error", cause);
        this.status = cause.getStatusCode();
        this.problem = problem(cause);
    }

    public static BffDownstreamException from(Throwable cause) {
        return cause instanceof WebClientResponseException response
                ? new BffDownstreamException(response)
                : new BffDownstreamException(cause);
    }

    public HttpStatusCode status() {
        return status;
    }

    public ProblemDetail problem() {
        return problem;
    }

    private static ProblemDetail problem(WebClientResponseException exception) {
        try {
            return new ObjectMapper().readValue(exception.getResponseBodyAsString(), ProblemDetail.class);
        } catch (Exception ignored) {
            return ProblemDetail.forStatusAndDetail(
                    exception.getStatusCode(), exception.getMessage());
        }
    }
}
