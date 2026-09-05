package com.adarsh.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.adarsh.core.ScenarioContext;
import com.adarsh.core.ScenarioContext.Key;
import com.adarsh.pages.AccountsOverviewPage;
import com.adarsh.pages.OpenNewAccountPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Step;
import java.math.BigDecimal;

/** Accounts Overview and Open New Account. */
public class AccountSteps {

    @Given("the customer notes the balance of their first account")
    @Step("Record the starting balance")
    public void noteFirstAccountBalance() {
        var overview = new AccountsOverviewPage();
        var account = overview.accountsTable().firstAccountNumber();
        ScenarioContext.put(Key.SOURCE_ACCOUNT, account);
        ScenarioContext.put(Key.BALANCE_BEFORE, overview.balanceOf(account));
    }

    @When("the customer opens a new {word} account")
    @Step("Open a new {0} account")
    public void openNewAccount(String accountType) {
        var page = new AccountsOverviewPage()
                .goToOpenNewAccount()
                .openAccount(OpenNewAccountPage.AccountType.valueOf(accountType.toUpperCase()));
        ScenarioContext.put(Key.NEW_ACCOUNT_NUMBER, page.newAccountNumber());
    }

    @Then("the new account is created with an account number")
    public void newAccountIsCreated() {
        var accountNumber = ScenarioContext.get(Key.NEW_ACCOUNT_NUMBER, String.class);
        assertThat(accountNumber)
                .as("ParaBank should return a numeric account number for the new account")
                .isNotBlank()
                .containsOnlyDigits();
    }

    @Then("the new account appears in the accounts overview")
    public void newAccountAppearsInOverview() {
        var accountNumber = ScenarioContext.get(Key.NEW_ACCOUNT_NUMBER, String.class);
        var overview = AccountsOverviewPage.openFromMenu();
        assertThat(overview.accountsTable().accountNumbers())
                .as("the newly opened account should be listed on the overview")
                .contains(accountNumber);
    }

    @Then("every listed account shows a balance")
    public void everyAccountShowsABalance() {
        var accounts = new AccountsOverviewPage().accounts();
        assertThat(accounts).as("accounts overview should list at least one account").isNotEmpty();
        assertThat(accounts)
                .as("no account row should be missing its account number")
                .allSatisfy(account -> assertThat(account.accountNumber()).isNotBlank());
    }

    @Then("the balance of the source account has decreased by {string}")
    public void balanceHasDecreasedBy(String amount) {
        var account = ScenarioContext.get(Key.SOURCE_ACCOUNT, String.class);
        var before = ScenarioContext.get(Key.BALANCE_BEFORE, BigDecimal.class);
        var after = AccountsOverviewPage.openFromMenu().balanceOf(account);

        assertThat(after)
                .as("balance of account %s should drop by %s", account, amount)
                .isEqualByComparingTo(before.subtract(new BigDecimal(amount)));
    }
}
