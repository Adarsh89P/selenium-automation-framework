package com.adarsh.domain;

import java.math.BigDecimal;

import org.testng.Assert;
import org.testng.annotations.Test;

public class AccountSummaryTest {

    @Test
    public void parsesPositiveCurrencyWithSymbolAndGrouping() {
        Assert.assertEquals(AccountSummary.parseCurrency("$1,234.56"), new BigDecimal("1234.56"));
    }

    @Test
    public void parsesNegativeCurrency() {
        Assert.assertEquals(AccountSummary.parseCurrency("-$50.00"), new BigDecimal("-50.00"));
    }

    @Test
    public void returnsZeroForMissingCurrency() {
        Assert.assertEquals(AccountSummary.parseCurrency(null), BigDecimal.ZERO);
        Assert.assertEquals(AccountSummary.parseCurrency("  "), BigDecimal.ZERO);
    }

    @Test
    public void trimsCurrencyBeforeParsing() {
        Assert.assertEquals(AccountSummary.parseCurrency("  $10.00  "), new BigDecimal("10.00"));
    }
}
