package com.adarsh.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.adarsh.config.ConfigReader;
import com.adarsh.core.ScenarioContext;
import com.adarsh.pages.AccountsOverviewPage;
import com.adarsh.pages.LoginPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Step;

/**
 * Login and logout.
 *
 * <p>Note what is absent: no {@code By}, no wait, no {@code driver}. Steps read as the business
 * intent and delegate the mechanics to the page objects.
 */
public class AuthenticationSteps {

    @Given("a customer is on the ParaBank login page")
    @Step("Open the ParaBank login page")
    public void openLoginPage() {
        LoginPage.open();
    }

    @When("the customer logs in with valid credentials")
    @Step("Log in with the configured customer")
    public void loginWithValidCredentials() {
        var config = ConfigReader.get();
        ScenarioContext.put(ScenarioContext.Key.LOGGED_IN_USERNAME, config.username());
        new LoginPage().loginAsConfiguredCustomer();
    }

    /** Background step used by every authenticated feature. */
    @Given("a logged-in customer")
    @Step("Log in as the configured customer")
    public void aLoggedInCustomer() {
        var config = ConfigReader.get();
        ScenarioContext.put(ScenarioContext.Key.LOGGED_IN_USERNAME, config.username());
        LoginPage.open().loginAsConfiguredCustomer();
    }

    @When("the customer logs in with username {string} and password {string}")
    @Step("Attempt login with username '{0}'")
    public void loginWith(String username, String password) {
        new LoginPage().loginExpectingFailure(username, password);
    }

    @When("the customer logs out")
    @Step("Log out")
    public void logOut() {
        new AccountsOverviewPage().logOut();
    }

    @Then("the accounts overview is displayed")
    public void accountsOverviewIsDisplayed() {
        var overview = new AccountsOverviewPage();
        assertThat(overview.accounts())
                .as("the logged-in customer should have at least one account listed")
                .isNotEmpty();
    }

    @Then("the customer is greeted by name")
    public void customerIsGreetedByName() {
        assertThat(new AccountsOverviewPage().loggedInUserName())
                .as("welcome banner should name the logged-in customer")
                .isNotBlank();
    }

    @Then("login is rejected with the message {string}")
    public void loginIsRejectedWith(String expectedMessage) {
        assertThat(new LoginPage().errorMessage())
                .as("login error message")
                .isEqualTo(expectedMessage);
    }

    @Then("the customer is returned to the login page")
    public void returnedToLoginPage() {
        assertThat(new LoginPage().isLoginFormVisible())
                .as("the login form should be visible again after logging out")
                .isTrue();
    }
}
