package com.adarsh.pages;

import com.adarsh.config.ConfigReader;
import com.adarsh.core.DriverManager;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Every wait, click, type, select, scroll and JS call in the framework lives here.
 *
 * <p>Page classes describe <em>what</em> to interact with; this class decides <em>how</em>. That
 * split is what keeps {@code Thread.sleep} out of the codebase: there is no page-level place to
 * put one, because pages never touch timing at all.
 */
public abstract class BasePage {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final WebDriver driver;
    private final WebDriverWait wait;

    protected BasePage() {
        this.driver = DriverManager.getDriver();
        this.wait = new WebDriverWait(driver, ConfigReader.explicitTimeout(), ConfigReader.pollingInterval());
    }

    /**
     * Each page states the one element that proves it has finished rendering. Constructors call
     * {@link #verifyLoaded()} so a page object can never hand back a half-drawn page.
     */
    protected abstract By pageReadyLocator();

    protected void verifyLoaded() {
        try {
            // "any match is displayed", not "the first match is displayed". ParaBank keeps a
            // page's form and its result panel as siblings and swaps which one is visible, so a
            // ready locator has to accept either state of the same page.
            waitUntil(d -> d.findElements(pageReadyLocator()).stream().anyMatch(WebElement::isDisplayed));
        } catch (org.openqa.selenium.TimeoutException e) {
            throw new IllegalStateException(
                    getClass().getSimpleName() + " did not load. Expected " + pageReadyLocator()
                            + " to be visible at " + driver.getCurrentUrl(),
                    e);
        }
    }

    // ===== Waits =====

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected WebElement waitForPresent(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    protected List<WebElement> waitForAllVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    protected boolean waitForInvisible(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    protected void waitForTextIn(By locator, String text) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    protected void waitForUrlContaining(String fragment) {
        wait.until(ExpectedConditions.urlContains(fragment));
    }

    /**
     * ParaBank fills several tables over AJAX, so "the table exists" is not the same as "the
     * table has rows". This waits for actual content.
     */
    protected List<WebElement> waitForNonEmptyRows(By rowLocator) {
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(rowLocator, 0));
        return driver.findElements(rowLocator);
    }

    /** Escape hatch for genuinely custom conditions, still bounded by the configured timeout. */
    protected <T> T waitUntil(ExpectedCondition<T> condition) {
        return wait.until(condition);
    }

    /** A wait that tolerates a mid-flight DOM swap, used for elements re-rendered by jQuery. */
    protected <T> T waitIgnoringStaleness(ExpectedCondition<T> condition) {
        return new FluentWait<>(driver)
                .withTimeout(ConfigReader.explicitTimeout())
                .pollingEvery(ConfigReader.pollingInterval())
                .ignoring(StaleElementReferenceException.class)
                .ignoring(NoSuchElementException.class)
                .until(condition);
    }

    // ===== Actions =====

    protected void click(By locator) {
        log.debug("Click {}", locator);
        waitForClickable(locator).click();
    }

    /** For controls that a sticky footer or overlay can intercept. */
    protected void jsClick(By locator) {
        var element = waitForPresent(locator);
        log.debug("JS click {}", locator);
        javascript().executeScript("arguments[0].click();", element);
    }

    protected void type(By locator, String text) {
        var element = waitForVisible(locator);
        element.clear();
        if (text != null && !text.isEmpty()) {
            element.sendKeys(text);
        }
        log.debug("Typed into {}", locator);
    }

    protected void selectByVisibleText(By locator, String text) {
        new Select(waitForVisible(locator)).selectByVisibleText(text);
        log.debug("Selected '{}' in {}", text, locator);
    }

    protected void selectByValue(By locator, String value) {
        new Select(waitForVisible(locator)).selectByValue(value);
    }

    /** Picks the first option, used where the exact account number is generated at runtime. */
    protected String selectFirstOption(By locator) {
        var select = new Select(waitForVisible(locator));
        waitUntil(d -> !select.getOptions().isEmpty());
        select.selectByIndex(0);
        return select.getFirstSelectedOption().getText().trim();
    }

    /**
     * Reads a dropdown's options, waiting for AJAX to fill it first. Reading immediately after
     * the {@code <select>} becomes visible returns an empty or half-filled list on ParaBank.
     */
    protected List<String> optionTexts(By locator) {
        var select = new Select(waitForVisible(locator));
        waitUntil(d -> !select.getOptions().isEmpty());
        return select.getOptions().stream().map(WebElement::getText).map(String::trim).toList();
    }

    protected void scrollIntoView(By locator) {
        var element = waitForPresent(locator);
        javascript().executeScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

    // ===== Reads =====

    protected String textOf(By locator) {
        return waitForVisible(locator).getText().trim();
    }

    protected String valueOf(By locator) {
        return waitForVisible(locator).getDomProperty("value");
    }

    protected boolean isDisplayed(By locator) {
        var found = driver.findElements(locator);
        return !found.isEmpty() && found.get(0).isDisplayed();
    }

    /** Non-throwing read for optional error banners. */
    protected Optional<String> optionalText(By locator) {
        return driver.findElements(locator).stream()
                .filter(WebElement::isDisplayed)
                .findFirst()
                .map(WebElement::getText)
                .map(String::trim);
    }

    // ===== Utilities =====

    protected JavascriptExecutor javascript() {
        return (JavascriptExecutor) driver;
    }

    /** Element-level screenshot, used to give a failed assertion visual context. */
    public byte[] elementScreenshot(By locator) {
        return waitForVisible(locator).getScreenshotAs(OutputType.BYTES);
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    public String pageTitle() {
        return driver.getTitle();
    }

    protected Duration timeout() {
        return ConfigReader.explicitTimeout();
    }
}
