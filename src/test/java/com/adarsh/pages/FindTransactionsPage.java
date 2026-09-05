package com.adarsh.pages;

import com.adarsh.domain.AccountSummary;
import com.adarsh.domain.TransactionRow;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/** Find Transactions, including its four separate search modes. */
public class FindTransactionsPage extends BasePage {

    private static final By FORM = By.id("transactionForm");
    private static final By ACCOUNT_SELECT = By.id("accountId");

    private static final By TRANSACTION_ID = By.id("transactionId");
    private static final By FIND_BY_ID = By.id("findById");

    private static final By TRANSACTION_DATE = By.id("transactionDate");
    private static final By FIND_BY_DATE = By.id("findByDate");

    private static final By FROM_DATE = By.id("fromDate");
    private static final By TO_DATE = By.id("toDate");
    private static final By FIND_BY_DATE_RANGE = By.id("findByDateRange");

    private static final By AMOUNT = By.id("amount");
    private static final By FIND_BY_AMOUNT = By.id("findByAmount");

    private static final By RESULT_CONTAINER = By.id("resultContainer");
    private static final By RESULT_ROWS = By.cssSelector("#transactionBody tr");
    private static final By ERROR_CONTAINER = By.id("errorContainer");

    // Field-level validation messages, each with its own span.
    private static final By ACCOUNT_ERROR = By.id("accountIdError");
    private static final By TRANSACTION_ID_ERROR = By.id("transactionIdError");
    private static final By DATE_ERROR = By.id("transactionDateError");
    private static final By DATE_RANGE_ERROR = By.id("dateRangeError");
    private static final By AMOUNT_ERROR = By.id("amountError");

    @Override
    protected By pageReadyLocator() {
        return FORM;
    }

    public FindTransactionsPage() {
        verifyLoaded();
    }

    /** Navigates here from any authenticated page via the Account Services menu. */
    public static FindTransactionsPage openFromMenu() {
        new com.adarsh.components.AccountServicesNav().findTransactions();
        return new FindTransactionsPage();
    }

    // ===== Actions =====

    public FindTransactionsPage forAccount(String accountNumber) {
        selectByVisibleText(ACCOUNT_SELECT, accountNumber);
        return this;
    }

    public String forFirstAccount() {
        return selectFirstOption(ACCOUNT_SELECT);
    }

    public FindTransactionsPage findByTransactionId(String transactionId) {
        type(TRANSACTION_ID, transactionId);
        click(FIND_BY_ID);
        return this;
    }

    /** ParaBank expects MM-DD-YYYY here. */
    public FindTransactionsPage findByDate(String date) {
        type(TRANSACTION_DATE, date);
        click(FIND_BY_DATE);
        return this;
    }

    public FindTransactionsPage findByDateRange(String fromDate, String toDate) {
        type(FROM_DATE, fromDate);
        type(TO_DATE, toDate);
        click(FIND_BY_DATE_RANGE);
        return this;
    }

    public FindTransactionsPage findByAmount(BigDecimal amount) {
        type(AMOUNT, amount.toPlainString());
        click(FIND_BY_AMOUNT);
        return this;
    }

    // ===== Reads =====

    /** Waits for the AJAX-populated result body before mapping rows to records. */
    public List<TransactionRow> results() {
        waitForVisible(RESULT_CONTAINER);
        var rows = waitForNonEmptyRows(RESULT_ROWS);
        return rows.stream().map(FindTransactionsPage::toRow).toList();
    }

    private static TransactionRow toRow(WebElement row) {
        var cells = row.findElements(By.tagName("td"));
        return new TransactionRow(
                cells.get(0).getText().trim(),
                cells.get(1).getText().trim(),
                AccountSummary.parseCurrency(cells.get(2).getText()),
                AccountSummary.parseCurrency(cells.get(3).getText()));
    }

    public boolean hasResults() {
        return isDisplayed(RESULT_CONTAINER) && !driver.findElements(RESULT_ROWS).isEmpty();
    }

    public boolean hasError() {
        return isDisplayed(ERROR_CONTAINER);
    }

    public Optional<String> accountError() {
        return optionalText(ACCOUNT_ERROR);
    }

    public Optional<String> transactionIdError() {
        return optionalText(TRANSACTION_ID_ERROR);
    }

    public Optional<String> dateError() {
        return optionalText(DATE_ERROR);
    }

    public Optional<String> dateRangeError() {
        return optionalText(DATE_RANGE_ERROR);
    }

    public Optional<String> amountError() {
        return optionalText(AMOUNT_ERROR);
    }

    /** Every validation message currently shown, used by the soft-assertion scenario. */
    public List<String> allValidationErrors() {
        return java.util.stream.Stream.of(
                        accountError(), transactionIdError(), dateError(), dateRangeError(), amountError())
                .flatMap(Optional::stream)
                .filter(text -> !text.isBlank())
                .toList();
    }
}
