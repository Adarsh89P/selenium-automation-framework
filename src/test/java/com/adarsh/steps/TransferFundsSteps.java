package com.adarsh.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.adarsh.core.ScenarioContext;
import com.adarsh.core.ScenarioContext.Key;
import com.adarsh.pages.AccountsOverviewPage;
import com.adarsh.pages.TransferFundsPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Step;
import java.math.BigDecimal;

/** Transfer Funds. */
public class TransferFundsSteps {

    @When("the customer transfers {string} between their own accounts")
    @Step("Transfer {0} between the customer's own accounts")
    public void transferBetweenOwnAccounts(String amount) {
        var transferPage = new AccountsOverviewPage().goToTransferFunds();

        var accounts = transferPage.availableFromAccounts();
        assertThat(accounts)
                .as("a transfer needs at least two accounts to move money between")
                .hasSizeGreaterThanOrEqualTo(2);

        var from = accounts.get(0);
        var to = accounts.get(1);

        ScenarioContext.put(Key.SOURCE_ACCOUNT, from);
        ScenarioContext.put(Key.TARGET_ACCOUNT, to);
        ScenarioContext.put(Key.TRANSFER_AMOUNT, new BigDecimal(amount));

        transferPage.transfer(new BigDecimal(amount), from, to);
    }

    @When("the customer submits a transfer of {string} without choosing accounts")
    @Step("Submit a transfer of {0} with no accounts chosen")
    public void submitTransferWithoutAccounts(String amount) {
        new AccountsOverviewPage()
                .goToTransferFunds()
                .enterAmount(new BigDecimal(amount))
                .submit();
    }

    @Then("the transfer is confirmed")
    public void transferIsConfirmed() {
        assertThat(new TransferFundsPage().isTransferComplete())
                .as("the Transfer Complete panel should be shown")
                .isTrue();
    }

    @Then("the confirmation shows the transferred amount and both accounts")
    public void confirmationShowsDetails() {
        var page = new TransferFundsPage();
        var expectedAmount = ScenarioContext.get(Key.TRANSFER_AMOUNT, BigDecimal.class);
        var expectedFrom = ScenarioContext.get(Key.SOURCE_ACCOUNT, String.class);
        var expectedTo = ScenarioContext.get(Key.TARGET_ACCOUNT, String.class);

        // Three related facts about one confirmation panel: collect all the mismatches rather
        // than stopping at the first, so a failure report explains the whole picture.
        org.assertj.core.api.SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(page.transferredAmount())
                    .as("amount shown on the confirmation")
                    .isEqualByComparingTo(expectedAmount);
            softly.assertThat(page.transferredFromAccount())
                    .as("source account shown on the confirmation")
                    .isEqualTo(expectedFrom);
            softly.assertThat(page.transferredToAccount())
                    .as("destination account shown on the confirmation")
                    .isEqualTo(expectedTo);
        });
    }

    @Then("the transfer is not completed")
    public void transferIsNotCompleted() {
        assertThat(new TransferFundsPage().hasError())
                .as("an incomplete transfer form should not report success")
                .isTrue();
    }
}
