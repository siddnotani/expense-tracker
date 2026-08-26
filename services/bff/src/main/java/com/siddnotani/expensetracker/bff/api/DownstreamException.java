package com.siddnotani.expensetracker.bff.api;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

class DownstreamException extends RuntimeException {
    private final HttpStatusCode status;
    private final ProblemDetail problem;

    DownstreamException(HttpStatusCode status, ProblemDetail problem) {
        super(problem.getDetail());
        this.status = status;
        this.problem = problem;
    }

    HttpStatusCode getStatus() {
        return status;
    }

    ProblemDetail getProblem() {
        return problem;
    }
}
