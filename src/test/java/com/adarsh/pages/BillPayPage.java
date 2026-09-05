package com.adarsh.pages;

import com.adarsh.domain.AccountSummary;
import com.adarsh.domain.Payee;
import java.math.BigDecimal;
import java.util.List;
import org.openqa.selenium.By;

/**
 * Bill Pay.
 *
 * <p>Worth reading the phone-number locator: ParaBank renders that input with a freshly generated
 * UUID id on every page load, so an id-based locator here would pass once and fail forever after.
 * The form is driven by {@code name} attributes, which are stable.
 */
public class BillPayPage extends BasePage {

    // Accepts either state of the page: the form before submit, the result panel after it.
    private static final By FORM = By.cssSelector("#billpayForm, #billpayResult, #billpayError");

    private static final By PAYEE_NAME = By.name("payee.name");
    private static final By PAYEE_STREET = By.name("payee.address.street");
    private static final By PAYEE_CITY = By.name("payee.address.city");
    private static final By PAYEE_STATE = By.name("payee.address.state");
    private static final By PAYEE_ZIP = By.name("payee.address.zipCode");
    private static final By PAYEE_PHONE = By.name("payee.phoneNumber");
    private static final By PAYEE_ACCOUNT = By.name("payee.accountNumber");
    private static final By VERIFY_ACCOUNT = By.name("verifyAccount");
    private static final By AMOUNT = By.name("amount");
    private static final By FROM_ACCOUNT = By.name("fromAccountId");
    private static final By SEND_BUTTON = By.cssSelector("#billpayForm input.button[value='Send Payment']");

    private static final By RESULT_PANEL = By.id("billpayResult");
    private static final By RESULT_PAYEE_NAME = By.id("payeeName");
    private static final By RESULT_AMOUNT = By.id("amount");
    private static final By RESULT_FROM_ACCOUNT = By.id("fromAccountId");
    private static final By ERROR_PANEL = By.id("billpayError");
    private static final By VALIDATION_ERRORS = By.cssSelector("#billpayForm span.error");

    @Override
    protected By pageReadyLocator() {
        return FORM;
    }

    public BillPayPage() {
        verifyLoaded();
    }

    // ===== Actions =====

    public BillPayPage fillPayee(Payee payee) {
        type(PAYEE_NAME, payee.name());
        type(PAYEE_STREET, payee.street());
        type(PAYEE_CITY, payee.city());
        type(PAYEE_STATE, payee.state());
        type(PAYEE_ZIP, payee.zipCode());
        type(PAYEE_PHONE, payee.phoneNumber());
        type(PAYEE_ACCOUNT, payee.accountNumber());
        type(VERIFY_ACCOUNT, payee.accountNumber());
        type(AMOUNT, payee.amount().toPlainString());
        return this;
    }

    /** Deliberately types a different confirmation number, for the mismatch scenario. */
    public BillPayPage withMismatchedVerifyAccount(String differentAccountNumber) {
        type(VERIFY_ACCOUNT, differentAccountNumber);
        return this;
    }

    public String chooseFirstSourceAccount() {
        return selectFirstOption(FROM_ACCOUNT);
    }

    public BillPayPage chooseSourceAccount(String accountNumber) {
        selectByVisibleText(FROM_ACCOUNT, accountNumber);
        return this;
    }

    public BillPayPage submit() {
        click(SEND_BUTTON);
        return this;
    }

    public BillPayPage payBill(Payee payee) {
        fillPayee(payee);
        var source = chooseFirstSourceAccount();
        log.info("Paying {} to '{}' from {}", payee.amount(), payee.name(), source);
        return submit();
    }

    // ===== Reads =====

    public boolean isPaymentComplete() {
        waitForVisible(RESULT_PANEL);
        return isDisplayed(RESULT_PANEL);
    }

    /**
     * Non-waiting variant for negative scenarios. The waiting version would spend the whole
     * timeout proving a panel that was never going to appear, and then throw the wrong error.
     */
    public boolean isPaymentPanelShown() {
        return isDisplayed(RESULT_PANEL);
    }

    public String paidPayeeName() {
        waitForVisible(RESULT_PANEL);
        return textOf(RESULT_PAYEE_NAME);
    }

    public BigDecimal paidAmount() {
        waitForVisible(RESULT_PANEL);
        return AccountSummary.parseCurrency(textOf(RESULT_AMOUNT));
    }

    public String paidFromAccount() {
        waitForVisible(RESULT_PANEL);
        return textOf(RESULT_FROM_ACCOUNT);
    }

    public boolean hasError() {
        return isDisplayed(ERROR_PANEL);
    }

    /** All visible field-level validation messages, for the soft-assertion scenario. */
    public List<String> validationErrors() {
        return driver.findElements(VALIDATION_ERRORS).stream()
                .filter(org.openqa.selenium.WebElement::isDisplayed)
                .map(org.openqa.selenium.WebElement::getText)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .toList();
    }

    public byte[] resultScreenshot() {
        return elementScreenshot(RESULT_PANEL);
    }
}
