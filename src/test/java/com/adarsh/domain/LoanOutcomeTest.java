package com.adarsh.domain;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LoanOutcomeTest {

    @Test
    public void approvedOutcomeContainsAccountNumber() {
        LoanOutcome outcome = LoanOutcome.approved("12345");

        Assert.assertEquals(outcome.status(), LoanOutcome.Status.APPROVED);
        Assert.assertEquals(outcome.accountNumber().orElseThrow(), "12345");
        Assert.assertTrue(outcome.message().contains("approved"));
    }

    @Test
    public void deniedOutcomeHasNoAccountNumber() {
        LoanOutcome outcome = LoanOutcome.denied("Insufficient funds");

        Assert.assertEquals(outcome.status(), LoanOutcome.Status.DENIED);
        Assert.assertFalse(outcome.accountNumber().isPresent());
        Assert.assertEquals(outcome.message(), "Insufficient funds");
    }

    @Test
    public void errorOutcomeHasNoAccountNumber() {
        LoanOutcome outcome = LoanOutcome.error("Service unavailable");

        Assert.assertEquals(outcome.status(), LoanOutcome.Status.ERROR);
        Assert.assertFalse(outcome.accountNumber().isPresent());
    }
}
