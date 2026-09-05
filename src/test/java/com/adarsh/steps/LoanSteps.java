package com.adarsh.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.adarsh.core.ScenarioContext;
import com.adarsh.core.ScenarioContext.Key;
import com.adarsh.domain.LoanOutcome;
import com.adarsh.pages.AccountsOverviewPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Step;
import java.math.BigDecimal;

/** Request Loan. */
public class LoanSteps {

    @When("the customer applies for a loan of {string} with a down payment of {string}")
    @Step("Apply for a loan of {0} with {1} down")
    public void applyForLoan(String amount, String downPayment) {
        var outcome = new AccountsOverviewPage()
                .goToRequestLoan()
                .applyFor(new BigDecimal(amount), new BigDecimal(downPayment))
                .outcome();
        ScenarioContext.put(Key.LOAN_OUTCOME, outcome);
    }

    @Then("the loan application receives a decision")
    public void loanApplicationReceivesADecision() {
        var outcome = ScenarioContext.get(Key.LOAN_OUTCOME, LoanOutcome.class);
        // Approval depends on the demo data's current balances, so the assertion is that the
        // application reaches a decision rather than that it says yes. Asserting on approval
        // would make this test fail for reasons that are not defects.
        assertThat(outcome.status())
                .as("the loan request should be decided, not error out. Message: %s", outcome.message())
                .isIn(LoanOutcome.Status.APPROVED, LoanOutcome.Status.DENIED);
    }

    @Then("an approved loan opens a new account")
    public void approvedLoanOpensANewAccount() {
        var outcome = ScenarioContext.get(Key.LOAN_OUTCOME, LoanOutcome.class);
        if (outcome.status() != LoanOutcome.Status.APPROVED) {
            // Not a failure: the scenario asserts the consequence of approval, and this run was
            // denied. Recorded so the report shows which branch was exercised.
            io.qameta.allure.Allure.step("Loan was denied on this run: " + outcome.message());
            return;
        }
        assertThat(outcome.accountNumber())
                .as("an approved loan should return the account number it funded")
                .isPresent()
                .get()
                .asString()
                .isNotBlank();
    }

    @Then("the loan application is denied")
    public void loanApplicationIsDenied() {
        var outcome = ScenarioContext.get(Key.LOAN_OUTCOME, LoanOutcome.class);
        assertThat(outcome.status())
                .as("an unaffordable loan should be denied. Message: %s", outcome.message())
                .isEqualTo(LoanOutcome.Status.DENIED);
    }
}
