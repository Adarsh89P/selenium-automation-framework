package com.adarsh.pages;

import com.adarsh.config.ConfigReader;
import java.util.Optional;
import org.openqa.selenium.By;

/**
 * ParaBank's customer login panel.
 *
 * <p>Note the locators: the login inputs carry no {@code id}, only {@code name}, so this page is
 * driven by name and by a scoped CSS selector rather than by an invented id.
 */
public class LoginPage extends BasePage {

    private static final By USERNAME = By.name("username");
    private static final By PASSWORD = By.name("password");
    private static final By LOGIN_BUTTON = By.cssSelector("#loginPanel input.button[value='Log In']");
    private static final By REGISTER_LINK = By.linkText("Register");
    private static final By FORGOT_LOGIN_LINK = By.linkText("Forgot login info?");
    private static final By ERROR_MESSAGE = By.cssSelector("#rightPanel p.error");
    private static final By ERROR_TITLE = By.cssSelector("#rightPanel h1.title");

    @Override
    protected By pageReadyLocator() {
        return LOGIN_BUTTON;
    }

    public LoginPage() {
        verifyLoaded();
    }

    /** Navigates to the login page from nowhere, then returns it ready to use. */
    public static LoginPage open() {
        com.adarsh.core.DriverManager.getDriver().get(ConfigReader.url("index.htm"));
        return new LoginPage();
    }

    // ===== Actions. Each returns the page the user actually lands on. =====

    /** The happy path: credentials that are expected to work. */
    public AccountsOverviewPage loginAs(String username, String password) {
        submitCredentials(username, password);
        return new AccountsOverviewPage();
    }

    /** Logs in with the configured customer, so scenarios do not repeat the credential lookup. */
    public AccountsOverviewPage loginAsConfiguredCustomer() {
        var config = ConfigReader.get();
        return loginAs(config.username(), config.password());
    }

    /**
     * The negative path. Returns {@code this} because a rejected login leaves the user on the
     * same page, and the caller then asserts on {@link #errorMessage()}.
     */
    public LoginPage loginExpectingFailure(String username, String password) {
        submitCredentials(username, password);
        waitForVisible(ERROR_TITLE);
        return this;
    }

    private void submitCredentials(String username, String password) {
        log.info("Logging in as '{}'", username);
        type(USERNAME, username);
        type(PASSWORD, password);
        click(LOGIN_BUTTON);
    }

    public RegisterPage goToRegister() {
        click(REGISTER_LINK);
        return new RegisterPage();
    }

    public void goToForgotLoginInfo() {
        click(FORGOT_LOGIN_LINK);
    }

    // ===== Reads =====

    public String errorMessage() {
        return textOf(ERROR_MESSAGE);
    }

    public Optional<String> maybeErrorMessage() {
        return optionalText(ERROR_MESSAGE);
    }

    public boolean isLoginFormVisible() {
        return isDisplayed(LOGIN_BUTTON);
    }

    /** Used by the visual-context screenshot on assertion failure. */
    public byte[] errorScreenshot() {
        return elementScreenshot(ERROR_MESSAGE);
    }
}
