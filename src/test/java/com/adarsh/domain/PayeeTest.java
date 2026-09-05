package com.adarsh.domain;

import java.math.BigDecimal;

import org.testng.Assert;
import org.testng.annotations.Test;

public class PayeeTest {

    @Test
    public void acceptsNullAndZeroAmounts() {
        Payee payee = new Payee("Payee", null, null, null, null, null, "123", null);

        Assert.assertNull(payee.amount());
        Assert.assertEquals(new Payee("Payee", null, null, null, null, null, "123",
                BigDecimal.ZERO).amount(), BigDecimal.ZERO);
    }

    @Test
    public void acceptsPositiveAmount() {
        Payee payee = new Payee("Payee", null, null, null, null, null, "123",
                new BigDecimal("12.50"));

        Assert.assertEquals(payee.amount(), new BigDecimal("12.50"));
    }

    @Test
    public void rejectsNegativeAmount() {
        var exception = Assert.expectThrows(IllegalArgumentException.class,
                () -> new Payee("Payee", null, null, null, null, null, "123",
                        new BigDecimal("-0.01")));
        Assert.assertTrue(exception.getMessage().contains("negative"));
    }
}
