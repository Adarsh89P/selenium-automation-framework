package com.adarsh.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.adarsh.components.AccountServicesNav;
import com.adarsh.core.ScenarioContext;
import com.adarsh.core.ScenarioContext.Key;
import com.adarsh.domain.Customer;
import com.adarsh.pages.LoginPage;
import com.adarsh.pages.RegisterPage;
import com.adarsh.utils.FakerUtils;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Step;

/** Customer registration. */
public class RegistrationSteps {

    @When("a new customer registers with valid details")
    @Step("Register a brand new customer")
    public void registerNewCustomer() {
        // Generated, not fixed: a hardcoded username registers successfully exactly once and
        // then fails on every subsequent run of the suite.
        var customer = FakerUtils.newCustomer();
        ScenarioContext.put(Key.NEW_CUSTOMER, customer);
        new LoginPage().goToRegister().register(customer);
    }

    @When("a new customer registers with a mismatched password confirmation")
    @Step("Register with a confirmation password that does not match")
    public void registerWithMismatchedPassword() {
        var customer = FakerUtils.newCustomer();
        ScenarioContext.put(Key.NEW_CUSTOMER, customer);
        new LoginPage()
                .goToRegister()
                .fill(customer)
                .withConfirmPassword(customer.password() + "-different")
                .submit();
    }

    @When("a new customer submits the registration form with every field empty")
    @Step("Submit an empty registration form")
    public void registerWithEmptyForm() {
        new LoginPage().goToRegister().submit();
    }

    @Then("the registration succeeds and the customer is signed in")
    public void registrationSucceeds() {
        var customer = ScenarioContext.get(Key.NEW_CUSTOMER, Customer.class);
        assertThat(new RegisterPage().isRegistrationSuccessful())
                .as("registration of '%s' should report success", customer.username())
                .isTrue();
        // ParaBank signs the new customer in but leaves them on register.htm rather than
        // redirecting to the overview, so the proof of a session is the Account Services menu
        // appearing, not the accounts table.
        assertThat(new AccountServicesNav().loggedInUserName())
                .as("the new customer should be signed in and named in the welcome banner")
                .contains(customer.firstName());
    }

    @Then("the registration is rejected with validation messages")
    public void registrationIsRejected() {
        assertThat(new RegisterPage().validationMessages())
                .as("an invalid registration should surface at least one validation message")
                .isNotEmpty();
    }
}
