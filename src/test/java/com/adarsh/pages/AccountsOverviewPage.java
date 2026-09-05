package com.adarsh.pages;

import com.adarsh.components.AccountServicesNav;
import com.adarsh.components.AccountsTable;
import com.adarsh.domain.AccountSummary;
import java.math.BigDecimal;
import java.util.List;
import org.openqa.selenium.By;

/** The landing page after a successful login. */
public class AccountsOverviewPage extends BasePage {

    private static final By TITLE = By.cssSelector("#showOverview h1.title, #rightPanel h1.title");
    private static final By ACCOUNT_TABLE = By.id("accountTable");

    private final AccountsTable accountsTable;
    private final AccountServicesNav nav;

    @Override
    protected By pageReadyLocator() {
        return ACCOUNT_TABLE;
    }

    public AccountsOverviewPage() {
        verifyLoaded();
        this.accountsTable = new AccountsTable();
        this.nav = new AccountServicesNav();
    }

    /**
     * Navigates to the overview from any authenticated page.
     *
     * <p>Use this rather than {@code new AccountsOverviewPage()} when the browser is somewhere
     * else: the constructor asserts the overview is already on screen, so calling it from, say,
     * the Open New Account page fails before it can navigate anywhere.
     */
    public static AccountsOverviewPage openFromMenu() {
        new AccountServicesNav().accountsOverview();
        return new AccountsOverviewPage();
    }

    // ===== Composition: the page exposes its widgets rather than re-implementing them. =====

    public AccountsTable accountsTable() {
        return accountsTable;
    }

    public AccountServicesNav nav() {
        return nav;
    }

    // ===== Reads =====

    public String heading() {
        return textOf(TITLE);
    }

    public List<AccountSummary> accounts() {
        return accountsTable.accounts();
    }

    public String loggedInUserName() {
        return nav.loggedInUserName();
    }

    public BigDecimal balanceOf(String accountNumber) {
        return accountsTable
                .findByNumber(accountNumber)
                .map(AccountSummary::balance)
                .orElseThrow(() -> new AssertionError(
                        "Account " + accountNumber + " is not listed on the overview. Present: "
                                + accountsTable.accountNumbers()));
    }

    public BigDecimal totalBalance() {
        return accounts().stream().map(AccountSummary::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ===== Navigation. Fluent chaining: each hop returns the next page object. =====

    public TransferFundsPage goToTransferFunds() {
        nav.transferFunds();
        return new TransferFundsPage();
    }

    public OpenNewAccountPage goToOpenNewAccount() {
        nav.openNewAccount();
        return new OpenNewAccountPage();
    }

    public BillPayPage goToBillPay() {
        nav.billPay();
        return new BillPayPage();
    }

    public FindTransactionsPage goToFindTransactions() {
        nav.findTransactions();
        return new FindTransactionsPage();
    }

    public UpdateProfilePage goToUpdateProfile() {
        nav.updateContactInfo();
        return new UpdateProfilePage().awaitProfileData();
    }

    public RequestLoanPage goToRequestLoan() {
        nav.requestLoan();
        return new RequestLoanPage();
    }

    public AccountsOverviewPage refresh() {
        nav.accountsOverview();
        return new AccountsOverviewPage();
    }

    public LoginPage logOut() {
        nav.logOut();
        return new LoginPage();
    }
}
