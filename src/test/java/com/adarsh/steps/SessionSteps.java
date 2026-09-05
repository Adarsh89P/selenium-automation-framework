package com.adarsh.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.adarsh.config.ConfigReader;
import com.adarsh.core.DriverManager;
import com.adarsh.pages.LoginPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Step;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Session-boundary behaviour: what happens after logout, and what happens when someone tries to
 * reach an authenticated page without a session.
 *
 * <p>These are the scenarios that catch real defects and that most portfolio suites skip.
 */
public class SessionSteps {

    @When("the customer presses the browser back button")
    @Step("Navigate back in browser history")
    public void pressBrowserBack() {
        DriverManager.getDriver().navigate().back();
    }

    @When("the customer navigates directly to the accounts overview")
    @Step("Request the accounts overview URL directly")
    public void navigateDirectlyToOverview() {
        DriverManager.getDriver().get(ConfigReader.url("overview.htm"));
    }

    /**
     * Re-requests account data from the server.
     *
     * <p>This step exists because ParaBank sends no {@code Cache-Control: no-store}, so pressing
     * back after logout renders a cached snapshot of the overview - headers, rows and all. That
     * snapshot proves nothing either way. Asking the server for the data again is what actually
     * tests whether the session is dead.
     */
    @When("the customer tries to reload their accounts from the cached page")
    @Step("Re-request account data from the server")
    public void reloadAccountsFromCachedPage() {
        DriverManager.getDriver().navigate().refresh();
    }

    @Then("the protected page is not shown")
    public void protectedPageIsNotShown() {
        var driver = DriverManager.getDriver();
        // Without a session ParaBank answers overview.htm with its error panel rather than
        // redirecting, so that panel - or the login form - is what proves the page is refused.
        new WebDriverWait(driver, ConfigReader.explicitTimeout())
                .until(ExpectedConditions.or(
                        ExpectedConditions.visibilityOfElementLocated(
                                org.openqa.selenium.By.cssSelector("#rightPanel p.error")),
                        ExpectedConditions.visibilityOfElementLocated(
                                org.openqa.selenium.By.cssSelector("#loginPanel"))));

        var accountRows = driver.findElements(
                org.openqa.selenium.By.cssSelector("#accountTable tbody tr"));
        assertThat(accountRows)
                .as("a session-less request must not return any account rows")
                .isEmpty();
    }

    @Then("the customer must log in again to continue")
    public void mustLogInAgain() {
        assertThat(new LoginPage().isLoginFormVisible())
                .as("the login form should be the only way forward after the session ends")
                .isTrue();
    }
}
