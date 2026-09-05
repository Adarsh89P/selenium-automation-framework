package com.adarsh.domain;

import java.math.BigDecimal;

/** A Bill Pay recipient plus the amount to send. */
public record Payee(
        String name,
        String street,
        String city,
        String state,
        String zipCode,
        String phoneNumber,
        String accountNumber,
        BigDecimal amount) {

    public Payee {
        if (amount != null && amount.signum() < 0) {
            throw new IllegalArgumentException("Bill payment amount cannot be negative: " + amount);
        }
    }
}
