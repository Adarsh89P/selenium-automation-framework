package com.adarsh.domain;

/**
 * A ParaBank customer, used both to register a new one and to update an existing profile.
 *
 * <p>The compact constructor rejects a half-built customer at the point of construction rather
 * than letting a null reach {@code sendKeys} and surface as an opaque WebDriver error.
 */
public record Customer(
        String firstName,
        String lastName,
        String street,
        String city,
        String state,
        String zipCode,
        String phoneNumber,
        String ssn,
        String username,
        String password) {

    public Customer {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("Customer requires a first name");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Customer requires a last name");
        }
    }

    /** Builder-ish helper for the common case of changing one field of an existing customer. */
    public Customer withPhoneNumber(String newPhoneNumber) {
        return new Customer(
                firstName, lastName, street, city, state, zipCode, newPhoneNumber, ssn, username,
                password);
    }

    public String fullName() {
        return firstName + " " + lastName;
    }
}
