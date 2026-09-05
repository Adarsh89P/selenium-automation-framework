package com.adarsh.components;

import com.adarsh.pages.BasePage;
import org.openqa.selenium.By;

/** The ParaBank masthead and the home/about/contact button strip, present on every page. */
public class HeaderComponent extends BasePage {

    private static final By LOGO = By.cssSelector("#topPanel img.logo");
    private static final By CAPTION = By.cssSelector("#topPanel p.caption");
    private static final By HOME_BUTTON = By.cssSelector("ul.button li.home a");
    private static final By ABOUT_BUTTON = By.cssSelector("ul.button li.aboutus a");
    private static final By CONTACT_BUTTON = By.cssSelector("ul.button li.contact a");
    private static final By ADMIN_LINK = By.xpath("//ul[@class='leftmenu']//a[normalize-space()='Admin Page']");

    @Override
    protected By pageReadyLocator() {
        return LOGO;
    }

    public HeaderComponent() {
        verifyLoaded();
    }

    public String tagline() {
        return textOf(CAPTION);
    }

    public String logoTitle() {
        return waitForVisible(LOGO).getDomAttribute("title");
    }

    public void goHome() {
        click(HOME_BUTTON);
    }

    public void goToAbout() {
        click(ABOUT_BUTTON);
    }

    public void goToContact() {
        click(CONTACT_BUTTON);
    }

    public void goToAdmin() {
        click(ADMIN_LINK);
    }
}
