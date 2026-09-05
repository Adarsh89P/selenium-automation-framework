package com.adarsh.pages;

import com.adarsh.domain.Customer;
import java.util.List;
import java.util.Optional;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Update Contact Info.
 *
 * <p>This page carries a full set of client-side validation spans, one per field, which makes it
 * the natural place to demonstrate soft assertions: a scenario can clear every field and check
 * all six messages in one pass instead of failing on the first.
 */
public class UpdateProfilePage extends BasePage {

    // Accepts either state of the page: the form before submit, the result panel after it.
    private static final By FORM = By.cssSelector("#updateProfileForm, #updateProfileResult, #updateProfileError");

    private static final By FIRST_NAME = By.id("customer.firstName");
    private static final By LAST_NAME = By.id("customer.lastName");
    private static final By STREET = By.id("customer.address.street");
    private static final By CITY = By.id("customer.address.city");
    private static final By STATE = By.id("customer.address.state");
    private static final By ZIP_CODE = By.id("customer.address.zipCode");
    private static final By PHONE = By.id("customer.phoneNumber");
    private static final By UPDATE_BUTTON = By.cssSelector("#updateProfileForm input.button[value='Update Profile']");

    private static final By RESULT_PANEL = By.id("updateProfileResult");
    private static final By ERROR_PANEL = By.id("updateProfileError");
    private static final By VALIDATION_MESSAGES = By.cssSelector("#updateProfileForm span.error");

    private static final By FIRST_NAME_ERROR = By.id("firstName-error");
    private static final By LAST_NAME_ERROR = By.id("lastName-error");
    private static final By STREET_ERROR = By.id("street-error");
    private static final By CITY_ERROR = By.id("city-error");
    private static final By STATE_ERROR = By.id("state-error");
    private static final By ZIP_CODE_ERROR = By.id("zipCode-error");

    @Override
    protected By pageReadyLocator() {
        return FORM;
    }

    public UpdateProfilePage() {
        verifyLoaded();
    }

    /**
     * ParaBank renders this form empty and fills it from an AJAX call a moment later.
     *
     * <p>Without this wait, clearing a field races the response: the framework blanks an already
     * empty input, the AJAX then writes the real value back, and the validation the scenario is
     * checking never fires. That produced a genuinely misleading failure - four of six messages
     * "missing".
     *
     * <p>It is a separate call rather than part of the constructor because after a submit the
     * fields are legitimately blank, and a constructor that insisted on data would then hang.
     * Only the navigation path waits.
     */
    public UpdateProfilePage awaitProfileData() {
        waitIgnoringStaleness(d -> {
            var value = d.findElement(FIRST_NAME).getDomProperty("value");
            return value != null && !value.isBlank();
        });
        return this;
    }

    // ===== Actions =====

    public UpdateProfilePage changePhoneNumber(String phoneNumber) {
        type(PHONE, phoneNumber);
        return this;
    }

    public UpdateProfilePage changeCity(String city) {
        type(CITY, city);
        return this;
    }

    /** Blanks every required field, to exercise the validation path. */
    public UpdateProfilePage clearRequiredFields() {
        List.of(FIRST_NAME, LAST_NAME, STREET, CITY, STATE, ZIP_CODE)
                .forEach(locator -> type(locator, ""));
        return this;
    }

    public UpdateProfilePage fillFrom(Customer customer) {
        type(FIRST_NAME, customer.firstName());
        type(LAST_NAME, customer.lastName());
        type(STREET, customer.street());
        type(CITY, customer.city());
        type(STATE, customer.state());
        type(ZIP_CODE, customer.zipCode());
        type(PHONE, customer.phoneNumber());
        return this;
    }

    public UpdateProfilePage submit() {
        click(UPDATE_BUTTON);
        return this;
    }

    // ===== Reads =====

    /** The profile as the application currently holds it, read back from the populated form. */
    public Customer currentProfile() {
        return new Customer(
                valueOf(FIRST_NAME),
                valueOf(LAST_NAME),
                valueOf(STREET),
                valueOf(CITY),
                valueOf(STATE),
                valueOf(ZIP_CODE),
                valueOf(PHONE),
                null,
                null,
                null);
    }

    public boolean isProfileUpdated() {
        waitForVisible(RESULT_PANEL);
        return isDisplayed(RESULT_PANEL);
    }

    public boolean hasError() {
        return isDisplayed(ERROR_PANEL);
    }

    public List<String> validationMessages() {
        return driver.findElements(VALIDATION_MESSAGES).stream()
                .filter(WebElement::isDisplayed)
                .map(WebElement::getText)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .toList();
    }

    public Optional<String> firstNameError() {
        return optionalText(FIRST_NAME_ERROR);
    }

    public Optional<String> lastNameError() {
        return optionalText(LAST_NAME_ERROR);
    }

    public Optional<String> streetError() {
        return optionalText(STREET_ERROR);
    }

    public Optional<String> cityError() {
        return optionalText(CITY_ERROR);
    }

    public Optional<String> stateError() {
        return optionalText(STATE_ERROR);
    }

    public Optional<String> zipCodeError() {
        return optionalText(ZIP_CODE_ERROR);
    }
}
