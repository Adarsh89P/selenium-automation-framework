package com.adarsh.components;

import com.adarsh.domain.AccountSummary;
import com.adarsh.pages.BasePage;
import java.util.List;
import java.util.Optional;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Reusable widget object for ParaBank's {@code #accountTable}.
 *
 * <p>ParaBank fills this table with an AJAX call to {@code services_proxy/bank/customers/{id}/
 * accounts}, so the table element exists in the HTML long before it has any rows. Reading it
 * without waiting for rows is the single most likely source of flakiness on this application,
 * which is why that wait lives here once rather than in every caller.
 */
public class AccountsTable extends BasePage {

    private static final By TABLE = By.id("accountTable");
    private static final By ROWS = By.cssSelector("#accountTable tbody tr");
    private static final By ACCOUNT_LINKS = By.cssSelector("#accountTable tbody tr td a");

    @Override
    protected By pageReadyLocator() {
        return TABLE;
    }

    public AccountsTable() {
        verifyLoaded();
    }

    /** Waits for the AJAX population to finish, then maps every row to a domain record. */
    public List<AccountSummary> accounts() {
        var rows = waitForNonEmptyRows(ROWS);
        return rows.stream().map(AccountsTable::toSummary).toList();
    }

    private static AccountSummary toSummary(WebElement row) {
        var cells = row.findElements(By.tagName("td"));
        return new AccountSummary(
                cells.get(0).getText().trim(),
                AccountSummary.parseCurrency(cells.get(1).getText()),
                AccountSummary.parseCurrency(cells.get(2).getText()));
    }

    public int rowCount() {
        return accounts().size();
    }

    public List<String> accountNumbers() {
        return accounts().stream().map(AccountSummary::accountNumber).toList();
    }

    public Optional<AccountSummary> findByNumber(String accountNumber) {
        return accounts().stream()
                .filter(account -> account.accountNumber().equals(accountNumber))
                .findFirst();
    }

    public boolean contains(String accountNumber) {
        return findByNumber(accountNumber).isPresent();
    }

    /** Opens the activity page for one account. */
    public void openAccount(String accountNumber) {
        waitForNonEmptyRows(ACCOUNT_LINKS);
        click(By.xpath("//table[@id='accountTable']//a[normalize-space()='" + accountNumber + "']"));
    }

    /** Convenience for tests that just need any usable account. */
    public String firstAccountNumber() {
        return accounts().stream()
                .findFirst()
                .map(AccountSummary::accountNumber)
                .orElseThrow(() -> new IllegalStateException(
                        "The logged-in customer has no accounts, so this scenario cannot run."));
    }
}
