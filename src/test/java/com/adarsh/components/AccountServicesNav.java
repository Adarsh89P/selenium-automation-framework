package com.adarsh.components;

import com.adarsh.pages.BasePage;
import org.openqa.selenium.By;

/**
 * The left-hand "Account Services" menu that appears on every authenticated ParaBank page.
 *
 * <p>It is a component rather than a base-page method because it is genuinely shared UI: keeping
 * it here means the menu's locators exist once, not once per page class.
 */
public class AccountServicesNav extends BasePage {

    private static final By PANEL = By.cssSelector("#leftPanel ul");
    private static final By WELCOME_TEXT = By.cssSelector("#leftPanel p.smallText");

    private static By menuItem(String linkText) {
        return By.xpath("//div[@id='leftPanel']//a[normalize-space()='" + linkText + "']");
    }

    @Override
    protected By pageReadyLocator() {
        return PANEL;
    }

    public AccountServicesNav() {
        verifyLoaded();
    }

    /** Reads "Welcome John Smith" and returns just the name. */
    public String loggedInUserName() {
        return textOf(WELCOME_TEXT).replaceFirst("(?i)^welcome\\s*", "").trim();
    }

    public boolean isVisible() {
        return isDisplayed(WELCOME_TEXT);
    }

    public void openNewAccount() {
        click(menuItem("Open New Account"));
    }

    public void accountsOverview() {
        click(menuItem("Accounts Overview"));
    }

    public void transferFunds() {
        click(menuItem("Transfer Funds"));
    }

    public void billPay() {
        click(menuItem("Bill Pay"));
    }

    public void findTransactions() {
        click(menuItem("Find Transactions"));
    }

    public void updateContactInfo() {
        click(menuItem("Update Contact Info"));
    }

    public void requestLoan() {
        click(menuItem("Request Loan"));
    }

    public void logOut() {
        click(menuItem("Log Out"));
    }
}
