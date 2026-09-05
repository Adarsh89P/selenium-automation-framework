package com.adarsh.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.adarsh.core.ScenarioContext;
import com.adarsh.core.ScenarioContext.Key;
import com.adarsh.domain.Payee;
import com.adarsh.pages.AccountsOverviewPage;
import com.adarsh.pages.BillPayPage;
import com.adarsh.utils.FakerUtils;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Step;
import java.math.BigDecimal;

/** Bill Pay. */
public class BillPaySteps {

    @When("the customer pays a bill of {string} to a new payee")
    @Step("Pay {0} to a freshly generated payee")
    public void payBillToNewPayee(String amount) {
        // The payee is generated rather than fixed: two parallel scenarios paying the same
        // named payee would otherwise be indistinguishable in the account history.
        var payee = FakerUtils.newPayee(new BigDecimal(amount));
        ScenarioContext.put(Key.PAYEE, payee);
        new AccountsOverviewPage().goToBillPay().payBill(payee);
    }

    @When("the customer pays a bill with a mismatched account confirmation")
    @Step("Submit a bill payment whose confirmation number does not match")
    public void payBillWithMismatchedConfirmation() {
        var payee = FakerUtils.newPayee(BigDecimal.TEN);
        ScenarioContext.put(Key.PAYEE, payee);
        new AccountsOverviewPage()
                .goToBillPay()
                .fillPayee(payee)
                .withMismatchedVerifyAccount(FakerUtils.numeric(6))
                .submit();
    }

    @Then("the bill payment is confirmed")
    public void billPaymentIsConfirmed() {
        assertThat(new BillPayPage().isPaymentComplete())
                .as("the Bill Payment Complete panel should be shown")
                .isTrue();
    }

    @Then("the confirmation names the payee and the amount")
    public void confirmationNamesPayeeAndAmount() {
        var page = new BillPayPage();
        var payee = ScenarioContext.get(Key.PAYEE, Payee.class);

        org.assertj.core.api.SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(page.paidPayeeName())
                    .as("payee named on the confirmation")
                    .isEqualTo(payee.name());
            softly.assertThat(page.paidAmount())
                    .as("amount shown on the confirmation")
                    .isEqualByComparingTo(payee.amount());
            softly.assertThat(page.paidFromAccount())
                    .as("source account shown on the confirmation")
                    .isNotBlank();
        });
    }

    @Then("the bill payment is rejected")
    public void billPaymentIsRejected() {
        var page = new BillPayPage();
        assertThat(page.isPaymentPanelShown())
                .as("a mismatched confirmation number should not produce a Payment Complete panel")
                .isFalse();
    }
}
