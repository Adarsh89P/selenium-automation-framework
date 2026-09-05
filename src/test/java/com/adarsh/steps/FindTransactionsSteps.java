package com.adarsh.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.adarsh.core.ScenarioContext;
import com.adarsh.core.ScenarioContext.Key;
import com.adarsh.pages.FindTransactionsPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Step;
import java.math.BigDecimal;

/** Find Transactions. */
public class FindTransactionsSteps {

    @When("the customer searches the debited account for transactions of {string}")
    @Step("Search the debited account for transactions of {0}")
    public void searchByAmount(String amount) {
        // openFromMenu() rather than the overview: this step follows a transfer, so the
        // browser is on transfer.htm and the overview page object would refuse to construct.
        var page = FindTransactionsPage.openFromMenu();

        // Search the account the transfer actually debited. Taking whichever account happens to
        // sit first in this dropdown looked equivalent and was not: ParaBank orders the two
        // dropdowns independently, so the search ran against an account with no such
        // transaction and reported an empty result as a product failure.
        ScenarioContext.find(Key.SOURCE_ACCOUNT, String.class)
                .ifPresentOrElse(page::forAccount, page::forFirstAccount);

        page.findByAmount(new BigDecimal(amount));
    }

    @When("the customer searches for transactions without entering any criteria")
    @Step("Submit the transaction search with no criteria")
    public void searchWithoutCriteria() {
        var page = FindTransactionsPage.openFromMenu();
        page.forFirstAccount();
        page.findByTransactionId("");
    }

    @Then("matching transactions are listed")
    public void matchingTransactionsAreListed() {
        var results = new FindTransactionsPage().results();
        assertThat(results).as("the search should return at least one transaction").isNotEmpty();
        assertThat(results)
                .as("every result row should carry a date and a description")
                .allSatisfy(row -> {
                    assertThat(row.date()).isNotBlank();
                    assertThat(row.description()).isNotBlank();
                });
    }

    @Then("a validation message explains what is missing")
    public void validationMessageExplainsWhatIsMissing() {
        assertThat(new FindTransactionsPage().allValidationErrors())
                .as("an empty search should be rejected with a field-level message")
                .isNotEmpty();
    }
}
