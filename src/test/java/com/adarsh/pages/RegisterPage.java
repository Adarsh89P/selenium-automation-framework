package com.adarsh.pages;

import com.adarsh.domain.Customer;
import java.util.List;
import java.util.Optional;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/** Customer registration. */
public class RegisterPage extends BasePage {

    // Accepts either state of the page: the registration form, or the confirmation ParaBank
    // renders in its place once the customer is signed up.
    private static final By FORM = By.cssSelector("#customerForm, #rightPanel h1.title");

    private static final By FIRST_NAME = By.id("customer.firstName");
    private static final By LAST_NAME = By.id("customer.lastName");
    private static final By STREET = By.id("customer.address.street");
    private static final By CITY = By.id("customer.address.city");
    private static final By STATE = By.id("customer.address.state");
    private static final By ZIP_CODE = By.id("customer.address.zipCode");
    private static final By PHONE = By.id("customer.phoneNumber");
    private static final By SSN = By.id("customer.ssn");
    private static final By USERNAME = By.id("customer.username");
    private static final By PASSWORD = By.id("customer.password");
    private static final By CONFIRM_PASSWORD = By.id("repeatedPassword");
    private static final By REGISTER_BUTTON = By.cssSelector("#customerForm input.button[value='Register']");

    private static final By WELCOME_HEADING = By.cssSelector("#rightPanel h1.title");
    private static final By SUCCESS_MESSAGE = By.xpath("//div[@id='rightPanel']//p[contains(., 'Your account was created successfully')]");
    private static final By VALIDATION_MESSAGES = By.cssSelector("#customerForm span.error");

    @Override
    protected By pageReadyLocator() {
        return FORM;
    }

    public RegisterPage() {
        verifyLoaded();
    }

    // ===== Actions =====

    public RegisterPage fill(Customer customer) {
        type(FIRST_NAME, customer.firstName());
        type(LAST_NAME, customer.lastName());
        type(STREET, customer.street());
        type(CITY, customer.city());
        type(STATE, customer.state());
        type(ZIP_CODE, customer.zipCode());
        type(PHONE, customer.phoneNumber());
        type(SSN, customer.ssn());
        type(USERNAME, customer.username());
        type(PASSWORD, customer.password());
        type(CONFIRM_PASSWORD, customer.password());
        return this;
    }

    /** For the mismatch scenario: overwrite only the confirmation field. */
    public RegisterPage withConfirmPassword(String confirmPassword) {
        type(CONFIRM_PASSWORD, confirmPassword);
        return this;
    }

    public RegisterPage submit() {
        click(REGISTER_BUTTON);
        return this;
    }

    /**
     * Registers the customer. ParaBank logs the new user straight in, so on success the caller
     * is already authenticated and can navigate on from the overview.
     */
    public RegisterPage register(Customer customer) {
        log.info("Registering new customer '{}'", customer.username());
        return fill(customer).submit();
    }

    // ===== Reads =====

    public boolean isRegistrationSuccessful() {
        return isDisplayed(SUCCESS_MESSAGE);
    }

    public String heading() {
        return textOf(WELCOME_HEADING);
    }

    public List<String> validationMessages() {
        return driver.findElements(VALIDATION_MESSAGES).stream()
                .filter(WebElement::isDisplayed)
                .map(WebElement::getText)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .toList();
    }

    public Optional<String> firstValidationMessage() {
        return validationMessages().stream().findFirst();
    }
}
