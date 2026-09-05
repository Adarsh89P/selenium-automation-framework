package com.adarsh.domain;

import org.testng.Assert;
import org.testng.annotations.Test;

public class CustomerTest {

    @Test
    public void fullNameCombinesFirstAndLastName() {
        Customer customer = customer();

        Assert.assertEquals(customer.fullName(), "Ada Lovelace");
    }

    @Test
    public void withPhoneNumberReturnsCustomerWithOnlyPhoneChanged() {
        Customer customer = customer();

        Customer updated = customer.withPhoneNumber("555-0100");

        Assert.assertEquals(updated.phoneNumber(), "555-0100");
        Assert.assertEquals(updated.firstName(), customer.firstName());
        Assert.assertEquals(updated.username(), customer.username());
    }

    @Test
    public void rejectsBlankFirstName() {
        var exception = Assert.expectThrows(IllegalArgumentException.class,
            () -> new Customer(" ", "Lovelace", null, null, null, null, null, null, null, null));
        Assert.assertTrue(exception.getMessage().contains("first name"));
    }

    @Test
    public void rejectsMissingLastName() {
        var exception = Assert.expectThrows(IllegalArgumentException.class,
            () -> new Customer("Ada", null, null, null, null, null, null, null, null, null));
        Assert.assertTrue(exception.getMessage().contains("last name"));
    }

    private static Customer customer() {
        return new Customer("Ada", "Lovelace", "1 Main St", "London", "State", "12345",
                "555-0000", "123-45-6789", "ada", "secret");
    }
}
