package com.siddnotani.expensetracker.bff;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class BffDownstreamException extends RuntimeException {
    private final HttpStatusCode status;

    public BffDownstreamException(Throwable cause) {
        super("Downstream service unavailable", cause);
        this.status = null;
    }

    private BffDownstreamException(WebClientResponseException cause) {
        super("Downstream service returned an error", cause);
        this.status = cause.getStatusCode();
    }

    public static BffDownstreamException from(Throwable cause) {
        return cause instanceof WebClientResponseException response
                ? new BffDownstreamException(response)
                : new BffDownstreamException(cause);
    }

    public HttpStatusCode status() {
        return status;
    }
}
