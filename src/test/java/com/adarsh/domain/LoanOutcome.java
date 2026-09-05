package com.adarsh.domain;

import java.util.Optional;

/**
 * The result of a loan application.
 *
 * <p>ParaBank renders approval and denial in two different panels, so the page object collapses
 * both into this one value and the step layer asserts on {@link Status} rather than on which div
 * happened to be visible.
 */
public record LoanOutcome(Status status, String newAccountNumber, String message) {

    public enum Status {
        APPROVED,
        DENIED,
        ERROR
    }

    public static LoanOutcome approved(String newAccountNumber) {
        return new LoanOutcome(Status.APPROVED, newAccountNumber, "Congratulations, your loan has been approved.");
    }

    public static LoanOutcome denied(String message) {
        return new LoanOutcome(Status.DENIED, null, message);
    }

    public static LoanOutcome error(String message) {
        return new LoanOutcome(Status.ERROR, null, message);
    }

    public Optional<String> accountNumber() {
        return Optional.ofNullable(newAccountNumber);
    }
}
