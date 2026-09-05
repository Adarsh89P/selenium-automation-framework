package com.adarsh.pages;

import java.math.BigDecimal;
import java.util.List;
import org.openqa.selenium.By;

/**
 * Transfer Funds.
 *
 * <p>Both account dropdowns are populated by AJAX after the page renders, so every read of them
 * goes through {@link BasePage#selectFirstOption} / {@link BasePage#optionTexts}, which wait for
 * options to exist. Selecting into an empty {@code <select>} is the classic ParaBank flake.
 */
public class TransferFundsPage extends BasePage {

    // Accepts either state of the page: the form before submit, the result panel after it.
    private static final By FORM = By.cssSelector("#transferForm, #showResult, #showError");
    private static final By AMOUNT = By.id("amount");
    private static final By FROM_ACCOUNT = By.id("fromAccountId");
    private static final By TO_ACCOUNT = By.id("toAccountId");
    private static final By TRANSFER_BUTTON = By.cssSelector("#transferForm input.button[value='Transfer']");

    private static final By RESULT_PANEL = By.id("showResult");
    private static final By RESULT_AMOUNT = By.id("amountResult");
    private static final By RESULT_FROM = By.id("fromAccountIdResult");
    private static final By RESULT_TO = By.id("toAccountIdResult");
    private static final By ERROR_PANEL = By.id("showError");

    @Override
    protected By pageReadyLocator() {
        return FORM;
    }

    public TransferFundsPage() {
        verifyLoaded();
    }

    // ===== Actions =====

    public TransferFundsPage enterAmount(BigDecimal amount) {
        type(AMOUNT, amount.toPlainString());
        return this;
    }

    public TransferFundsPage fromAccount(String accountNumber) {
        selectByVisibleText(FROM_ACCOUNT, accountNumber);
        return this;
    }

    public TransferFundsPage toAccount(String accountNumber) {
        selectByVisibleText(TO_ACCOUNT, accountNumber);
        return this;
    }

    /** Picks the first available account, for scenarios that only need "some" account. */
    public String selectFirstFromAccount() {
        return selectFirstOption(FROM_ACCOUNT);
    }

    public String selectFirstToAccount() {
        return selectFirstOption(TO_ACCOUNT);
    }

    public List<String> availableFromAccounts() {
        return optionTexts(FROM_ACCOUNT);
    }

    public TransferFundsPage submit() {
        click(TRANSFER_BUTTON);
        return this;
    }

    /** The full happy path in one call, since every transfer scenario needs the same three steps. */
    public TransferFundsPage transfer(BigDecimal amount, String fromAccount, String toAccount) {
        log.info("Transferring {} from {} to {}", amount, fromAccount, toAccount);
        return enterAmount(amount).fromAccount(fromAccount).toAccount(toAccount).submit();
    }

    // ===== Reads =====

    public boolean isTransferComplete() {
        waitForVisible(RESULT_PANEL);
        return isDisplayed(RESULT_PANEL);
    }

    public BigDecimal transferredAmount() {
        waitForVisible(RESULT_PANEL);
        return com.adarsh.domain.AccountSummary.parseCurrency(textOf(RESULT_AMOUNT));
    }

    public String transferredFromAccount() {
        waitForVisible(RESULT_PANEL);
        return textOf(RESULT_FROM);
    }

    public String transferredToAccount() {
        waitForVisible(RESULT_PANEL);
        return textOf(RESULT_TO);
    }

    public boolean hasError() {
        return isDisplayed(ERROR_PANEL);
    }

    /** Element screenshot of the result panel, attached to the report for visual context. */
    public byte[] resultScreenshot() {
        return elementScreenshot(RESULT_PANEL);
    }
}
