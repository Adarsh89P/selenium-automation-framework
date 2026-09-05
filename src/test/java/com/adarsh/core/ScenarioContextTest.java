package com.adarsh.core;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ScenarioContextTest {

    @BeforeMethod
    public void setUp() {
        ScenarioContext.clear();
    }

    @AfterMethod
    public void tearDown() {
        ScenarioContext.clear();
    }

    @Test
    public void storesAndReadsTypedValues() {
        ScenarioContext.put(ScenarioContext.Key.NEW_ACCOUNT_NUMBER, "12345");

        Assert.assertTrue(ScenarioContext.has(ScenarioContext.Key.NEW_ACCOUNT_NUMBER));
        Assert.assertEquals(ScenarioContext.get(ScenarioContext.Key.NEW_ACCOUNT_NUMBER, String.class),
                "12345");
        Assert.assertEquals(ScenarioContext.find(ScenarioContext.Key.NEW_ACCOUNT_NUMBER, String.class)
                .orElseThrow(), "12345");
    }

    @Test
    public void findReturnsEmptyForMissingValue() {
        Assert.assertFalse(ScenarioContext.find(ScenarioContext.Key.PAYEE, String.class).isPresent());
        Assert.assertFalse(ScenarioContext.has(ScenarioContext.Key.PAYEE));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void getFailsWhenValueIsMissing() {
        ScenarioContext.get(ScenarioContext.Key.PAYEE, String.class);
    }

    @Test
    public void clearRemovesStoredValues() {
        ScenarioContext.put(ScenarioContext.Key.PAYEE, "payee");

        ScenarioContext.clear();

        Assert.assertFalse(ScenarioContext.has(ScenarioContext.Key.PAYEE));
    }
}
