package com.adarsh.utils;

import com.adarsh.config.ConfigReader;
import com.adarsh.core.DriverManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The framework's waiting primitives.
 *
 * <h2>How this relates to BasePage</h2>
 *
 * <p>{@code BasePage} already exposes the element-level waits that page objects need, and this
 * class does not repeat them - a second {@code waitForVisible} would be two places to change a
 * timeout and two places for them to disagree. What lives here is the layer underneath: the code
 * that knows how a wait is <em>built</em> (timeout, polling interval, which exceptions are
 * tolerated), plus the waits that are not about a single element and therefore have no home on a
 * page object.
 *
 * <p>{@code BasePage} builds its waits by calling into here, so the timeout configuration is read
 * in exactly one place.
 *
 * <h2>Who else uses it</h2>
 *
 * <p>Everything that waits but is not a page: data providers polling for a seeded record, the
 * download-verification scenario waiting on a file, hooks waiting for a page to settle before
 * screenshotting. Those have no {@code BasePage} instance, and before this class existed the only
 * thing they could reach for was {@code Thread.sleep}.
 *
 * <h2>Why there is no sleep anywhere in here</h2>
 *
 * <p>Including in {@link #forFile(Path)}, which polls the filesystem rather than the browser.
 * {@link FluentWait} is generic over its input - it is not tied to a {@code WebDriver} - so a
 * filesystem or API poll gets the same bounded, interval-controlled, properly-messaged wait that a
 * browser poll does. A fixed sleep is either too short (flaky) or too long (slow), and it is
 * usually both in the same suite.
 */
public final class WaitUtils {

    private static final Logger log = LoggerFactory.getLogger(WaitUtils.class);

    private WaitUtils() {
        // utility holder
    }

    // ===== Wait builders. The single place that reads timeout configuration. =====

    /** A standard explicit wait on the current thread's driver. */
    public static WebDriverWait standard() {
        return new WebDriverWait(
                DriverManager.getDriver(),
                ConfigReader.explicitTimeout(),
                ConfigReader.pollingInterval());
    }

    /**
     * A wait that survives a DOM swap mid-poll.
     *
     * <p>Needed wherever a framework re-renders a region while the wait is running: the element
     * matched on one poll is detached by the next, and the resulting
     * {@code StaleElementReferenceException} is not a failure, just bad timing. Ignoring it lets
     * the wait re-find the element rather than the test dying on a race it does not care about.
     */
    public static FluentWait<WebDriver> tolerant() {
        return new FluentWait<>(DriverManager.getDriver())
                .withTimeout(ConfigReader.explicitTimeout())
                .pollingEvery(ConfigReader.pollingInterval())
                .ignoring(StaleElementReferenceException.class)
                .ignoring(NoSuchElementException.class);
    }

    public static <T> T until(ExpectedCondition<T> condition) {
        return standard().until(condition);
    }

    public static <T> T untilTolerant(Function<WebDriver, T> condition) {
        return tolerant().until(condition);
    }

    // ===== Page-level readiness =====

    /**
     * Waits for {@code document.readyState == "complete"}.
     *
     * <p>Rarely the right tool on its own: a single-page application reaches "complete" long before
     * it has rendered anything, which is why every page object still declares its own ready
     * locator. It earns its place after a full page transition - a form post, a redirect - where
     * the browser genuinely is loading a new document.
     */
    public static void forDocumentReady() {
        until(driver -> "complete".equals(
                ((JavascriptExecutor) driver).executeScript("return document.readyState")));
    }

    /**
     * Waits for jQuery to have no requests in flight.
     *
     * <p>Returns immediately when the page has no jQuery, rather than failing. That matters: a
     * generic quiescence wait that throws on a plain HTML page would push callers back to a sleep,
     * which is the thing this class exists to prevent.
     */
    public static void forAjaxQuiet() {
        untilTolerant(driver -> {
            var active = ((JavascriptExecutor) driver).executeScript(
                    "return (window.jQuery && jQuery.active !== undefined) ? jQuery.active : 0;");
            return active instanceof Number number && number.longValue() == 0L;
        });
    }

    // ===== Waits that have nothing to do with the browser =====

    /**
     * Waits for a file to exist and stop growing.
     *
     * <p>Existence alone is not enough for a download check: the browser creates the target file
     * the moment the transfer starts, so a test that asserts on {@code Files.exists} can pass
     * against a zero-byte file, or read a half-written one. This requires the size to be non-zero
     * and unchanged across two consecutive polls before it returns.
     *
     * @return the same path, so it can be used inline in an assertion
     */
    public static Path forFile(Path file) {
        return forFile(file, ConfigReader.explicitTimeout());
    }

    public static Path forFile(Path file, Duration timeout) {
        // FluentWait over the Path itself: no WebDriver involved, but the same bounded polling and
        // the same readable timeout message.
        var sizes = new long[] {-1L};
        new FluentWait<>(file)
                .withTimeout(timeout)
                .pollingEvery(ConfigReader.pollingInterval())
                .withMessage(() -> "File never finished downloading: " + file.toAbsolutePath())
                .until(path -> {
                    if (!Files.isRegularFile(path)) {
                        return false;
                    }
                    long current;
                    try {
                        current = Files.size(path);
                    } catch (java.io.IOException e) {
                        // Mid-write on Windows: the file is locked. Not a failure, just early.
                        return false;
                    }
                    var settled = current > 0 && current == sizes[0];
                    sizes[0] = current;
                    return settled;
                });
        log.debug("File is complete: {}", file);
        return file;
    }

    /**
     * Waits for an arbitrary condition that does not involve the browser.
     *
     * <p>The intended caller is the API layer polling for a record it has just asked the
     * application to create. The description is required rather than optional because the whole
     * value of this over a loop-and-sleep is the message you get when it times out: "timed out
     * waiting for" plus a sentence beats "expected true but was false".
     */
    public static void forCondition(BooleanSupplier condition, String description) {
        forCondition(condition, description, ConfigReader.explicitTimeout());
    }

    public static void forCondition(BooleanSupplier condition, String description, Duration timeout) {
        new FluentWait<>(condition)
                .withTimeout(timeout)
                .pollingEvery(ConfigReader.pollingInterval())
                .withMessage(() -> "Timed out waiting for " + description)
                .until(BooleanSupplier::getAsBoolean);
    }
}
