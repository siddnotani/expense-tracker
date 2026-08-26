package com.siddnotani.expensetracker.bff.api;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

public class DownstreamException extends RuntimeException {
    private final HttpStatusCode status;
    private final ProblemDetail problem;

    public DownstreamException(HttpStatusCode status, ProblemDetail problem) {
        super(problem.getDetail());
        this.status = status;
        this.problem = problem;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public ProblemDetail getProblem() {
        return problem;
    }
}
