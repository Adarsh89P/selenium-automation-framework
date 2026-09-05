package com.adarsh.pages;

import com.adarsh.domain.LoanOutcome;
import java.math.BigDecimal;
import org.openqa.selenium.By;

/**
 * Request Loan.
 *
 * <p>The application renders approval and denial into two different panels. This page collapses
 * both into a {@link LoanOutcome}, so the step layer asserts on a status rather than on which div
 * happens to be showing.
 */
public class RequestLoanPage extends BasePage {

    // Accepts either state of the page: the form before submit, the result panel after it.
    private static final By FORM = By.cssSelector("#requestLoanForm, #requestLoanResult, #requestLoanError");
    private static final By AMOUNT = By.id("amount");
    private static final By DOWN_PAYMENT = By.id("downPayment");
    private static final By FROM_ACCOUNT = By.id("fromAccountId");
    private static final By APPLY_BUTTON = By.cssSelector("#requestLoanForm input.button[value='Apply Now']");

    private static final By RESULT_PANEL = By.id("requestLoanResult");
    private static final By LOAN_STATUS = By.id("loanStatus");
    private static final By APPROVED_PANEL = By.id("loanRequestApproved");
    private static final By DENIED_PANEL = By.id("loanRequestDenied");
    private static final By DENIED_MESSAGE = By.cssSelector("#loanRequestDenied p.error");
    private static final By NEW_ACCOUNT_ID = By.id("newAccountId");
    private static final By ERROR_PANEL = By.id("requestLoanError");

    @Override
    protected By pageReadyLocator() {
        return FORM;
    }

    public RequestLoanPage() {
        verifyLoaded();
    }

    // ===== Actions =====

    public RequestLoanPage enterLoanAmount(BigDecimal amount) {
        type(AMOUNT, amount.toPlainString());
        return this;
    }

    public RequestLoanPage enterDownPayment(BigDecimal downPayment) {
        type(DOWN_PAYMENT, downPayment.toPlainString());
        return this;
    }

    public String chooseFirstFundingAccount() {
        return selectFirstOption(FROM_ACCOUNT);
    }

    public RequestLoanPage submit() {
        click(APPLY_BUTTON);
        return this;
    }

    public RequestLoanPage applyFor(BigDecimal amount, BigDecimal downPayment) {
        enterLoanAmount(amount);
        enterDownPayment(downPayment);
        var funding = chooseFirstFundingAccount();
        log.info("Applying for a loan of {} with {} down, from account {}", amount, downPayment, funding);
        return submit();
    }

    // ===== Reads =====

    /** Waits for whichever outcome panel the application decides to show. */
    public LoanOutcome outcome() {
        waitUntil(d -> isDisplayed(RESULT_PANEL) || isDisplayed(ERROR_PANEL));

        if (isDisplayed(ERROR_PANEL)) {
            return LoanOutcome.error(textOf(By.cssSelector("#requestLoanError p.error")));
        }
        if (isDisplayed(APPROVED_PANEL)) {
            return LoanOutcome.approved(textOf(NEW_ACCOUNT_ID));
        }
        if (isDisplayed(DENIED_PANEL)) {
            return LoanOutcome.denied(optionalText(DENIED_MESSAGE).orElse("Loan request denied."));
        }
        // The panel is up but neither branch rendered: report what the page actually says.
        return LoanOutcome.error("Unrecognised loan outcome. Status text: " + optionalText(LOAN_STATUS).orElse("<empty>"));
    }

    public String loanStatusText() {
        waitForVisible(RESULT_PANEL);
        return optionalText(LOAN_STATUS).orElse("");
    }

    public byte[] resultScreenshot() {
        return elementScreenshot(RESULT_PANEL);
    }
}
