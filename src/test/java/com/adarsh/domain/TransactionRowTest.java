package com.adarsh.domain;

import java.math.BigDecimal;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TransactionRowTest {

    @Test
    public void positiveCreditIsCredit() {
        TransactionRow row = new TransactionRow("today", "Deposit", null, new BigDecimal("10.00"));

        Assert.assertTrue(row.isCredit());
        Assert.assertFalse(row.isDebit());
    }

    @Test
    public void positiveDebitIsDebit() {
        TransactionRow row = new TransactionRow("today", "Payment", new BigDecimal("10.00"), null);

        Assert.assertTrue(row.isDebit());
        Assert.assertFalse(row.isCredit());
    }

    @Test
    public void nullAndZeroAmountsAreNeitherCreditNorDebit() {
        TransactionRow row = new TransactionRow("today", "Adjustment", BigDecimal.ZERO, null);

        Assert.assertFalse(row.isCredit());
        Assert.assertFalse(row.isDebit());
    }
}
